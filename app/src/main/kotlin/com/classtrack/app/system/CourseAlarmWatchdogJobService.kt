package com.classtrack.app.system

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.classtrack.app.data.DataRepository

/**
 * 系统级后台巡检看门狗 (JobScheduler Watchdog)
 *
 * 每 15 分钟在系统允许的省电窗口触发一次，即使用户长期未打开《课轨》，也负责：
 * 1. 自动校验并补齐未来 7 天 + 跨周自动顺延的硬件级 RTC 闹钟链；
 * 2. 若当前正处于课前提醒窗口或上课进行中、且用户未点击「已到教室」收起，自动同步/补发实时流体云卡片。
 */
class CourseAlarmWatchdogJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        runCatching {
            val appContext = applicationContext
            val repository = DataRepository(appContext)
            if (repository.fluidCloudEnabled.value) {
                CourseAlarmScheduler.rescheduleAll(appContext)
                ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                    context = appContext,
                    isSilentUpdate = true
                )
            }
        }
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // 系统抢占中断时请求重新调度，保证巡检链不断裂
        return true
    }

    companion object {
        private const val WATCHDOG_JOB_ID = 41001
        private const val WATCHDOG_INTERVAL_MS = 15L * 60L * 1000L

        fun ensureScheduled(context: Context) {
            val appContext = context.applicationContext
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val existing = jobScheduler.getPendingJob(WATCHDOG_JOB_ID)
            if (existing != null) return

            val jobInfo = JobInfo.Builder(
                WATCHDOG_JOB_ID,
                ComponentName(appContext, CourseAlarmWatchdogJobService::class.java)
            )
                .setPeriodic(WATCHDOG_INTERVAL_MS)
                .setPersisted(true)
                .build()

            runCatching {
                jobScheduler.schedule(jobInfo)
            }
        }

        fun cancel(context: Context) {
            val appContext = context.applicationContext
            val jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            runCatching {
                jobScheduler.cancel(WATCHDOG_JOB_ID)
            }
        }
    }
}
