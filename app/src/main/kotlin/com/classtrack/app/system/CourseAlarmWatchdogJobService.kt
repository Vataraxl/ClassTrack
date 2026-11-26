package com.classtrack.app.system

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.classtrack.app.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 系统级后台巡检看门狗 (JobScheduler Watchdog)
 *
 * 修复旧版同步调用 jobFinished 却 return false 导致在 Android 17 上残留僵尸任务的契约缺陷，
 * 并增加设备开机未解锁保护（延迟重试直到用户解锁屏幕后安全读取本地加密数据）。
 */
class CourseAlarmWatchdogJobService : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        val appContext = applicationContext
        val userUnlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appContext.getSystemService(UserManager::class.java)?.isUserUnlocked ?: true
        } else {
            true
        }

        if (!userUnlocked) {
            Log.i(TAG, "Watchdog 触发时用户尚未解锁设备，安排解锁后重试")
            scheduleUnlockRetry(appContext)
            return false
        }

        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                val repository = DataRepository.getInstance(appContext)
                if (repository.fluidCloudEnabled.value) {
                    CourseAlarmScheduler.rescheduleAll(appContext)
                    ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                        context = appContext,
                        isSilentUpdate = true
                    )
                    Log.i(TAG, "Watchdog 后台巡检完成 (jobId=${params?.jobId})")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Watchdog 执行失败: ${t.message}", t)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        activeJob?.cancel()
        return true
    }

    companion object {
        private const val TAG = "ClassTrackAlarm"
        private const val WATCHDOG_JOB_ID = 41001
        private const val UNLOCK_RETRY_JOB_ID = 41002
        private const val WATCHDOG_INTERVAL_MS = 15L * 60L * 1000L

        fun ensureScheduled(context: Context, forceRefresh: Boolean = false) {
            val appContext = context.applicationContext
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            if (forceRefresh) {
                runCatching { jobScheduler.cancel(WATCHDOG_JOB_ID) }
            } else {
                val existing = jobScheduler.getPendingJob(WATCHDOG_JOB_ID)
                if (existing != null) return
            }

            val jobInfo = JobInfo.Builder(
                WATCHDOG_JOB_ID,
                ComponentName(appContext, CourseAlarmWatchdogJobService::class.java)
            )
                .setPeriodic(WATCHDOG_INTERVAL_MS)
                .setPersisted(true)
                .build()

            runCatching {
                val result = jobScheduler.schedule(jobInfo)
                Log.i(TAG, "注册周期巡检 Watchdog (force=$forceRefresh): result=$result")
            }.onFailure { error ->
                Log.e(TAG, "注册周期巡检 Watchdog 失败: ${error.message}", error)
            }
        }

        fun scheduleUnlockRetry(context: Context) {
            val appContext = context.applicationContext
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val jobInfo = JobInfo.Builder(
                UNLOCK_RETRY_JOB_ID,
                ComponentName(appContext, CourseAlarmWatchdogJobService::class.java)
            )
                .setMinimumLatency(60_000L)
                .setOverrideDeadline(180_000L)
                .setPersisted(false)
                .build()

            runCatching {
                jobScheduler.schedule(jobInfo)
            }.onFailure { error ->
                Log.w(TAG, "安排解锁重试任务失败: ${error.message}")
            }
        }

        fun cancel(context: Context) {
            val appContext = context.applicationContext
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            runCatching {
                jobScheduler.cancel(WATCHDOG_JOB_ID)
                jobScheduler.cancel(UNLOCK_RETRY_JOB_ID)
            }
        }
    }
}
