package com.classtrack.app.ui.main

import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.system.LiveCapsuleOsFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsHelperTest {

    @Test
    fun parseAndFormatHhMm_handlesValidAndInvalidInputs() {
        assertEquals(8 * 60, parseHhMmToMinutes("08:00"))
        assertEquals(23 * 60 + 59, parseHhMmToMinutes("23:59"))
        assertNull(parseHhMmToMinutes("24:00"))
        assertNull(parseHhMmToMinutes("08:60"))
        assertNull(parseHhMmToMinutes("abc"))
        assertEquals("09:05", formatMinutesToHhMm(9 * 60 + 5))
        assertEquals("23:59", formatMinutesToHhMm(25 * 60))
    }

    @Test
    fun applyTimeSlotUpdate_updatesSingleOrCascadesSubsequentSections() {
        val initialSlots = listOf(
            TimeSlot(1, "08:00", "08:45"),
            TimeSlot(2, "08:55", "09:40"),
            TimeSlot(3, "10:00", "10:45")
        )

        val singleOnly = applyTimeSlotUpdate(
            slots = initialSlots,
            section = 1,
            newStart = "08:15",
            newEnd = "09:00",
            cascadeSubsequent = false
        )
        assertEquals("08:15", singleOnly[0].startTime)
        assertEquals("09:00", singleOnly[0].endTime)
        assertEquals("08:55", singleOnly[1].startTime)
        assertEquals("10:00", singleOnly[2].startTime)

        val cascaded = applyTimeSlotUpdate(
            slots = initialSlots,
            section = 1,
            newStart = "08:15",
            newEnd = "09:00",
            cascadeSubsequent = true
        )
        assertEquals("08:15", cascaded[0].startTime)
        assertEquals("09:00", cascaded[0].endTime)
        assertEquals("09:10", cascaded[1].startTime)
        assertEquals("09:55", cascaded[1].endTime)
        assertEquals("10:15", cascaded[2].startTime)
        assertEquals("11:00", cascaded[2].endTime)
    }

    @Test
    fun resolveOemGuidance_adaptsToDeviceBrandWithoutLockingNonOppoDevices() {
        val oppo = resolveOemGuidance("OPPO", "OPPO", LiveCapsuleOsFamily.COLOROS)
        assertTrue(oppo.osDisplayLabel.contains("ColorOS"))
        assertTrue(oppo.watchCompanionTip.contains("欢太健康"))

        val xiaomi = resolveOemGuidance("Xiaomi", "Redmi", LiveCapsuleOsFamily.HYPEROS)
        assertTrue(xiaomi.osDisplayLabel.contains("HyperOS"))
        assertTrue(xiaomi.watchCompanionTip.contains("小米运动健康"))

        val vivo = resolveOemGuidance("vivo", "iQOO", LiveCapsuleOsFamily.ORIGINOS)
        assertTrue(vivo.osDisplayLabel.contains("OriginOS"))
        assertTrue(vivo.autoStartTip.contains("i 管家"))

        val pixel = resolveOemGuidance("Google", "Pixel", LiveCapsuleOsFamily.COLOROS)
        assertEquals("Android 设备", pixel.osDisplayLabel)
        assertTrue(pixel.watchCompanionTip.contains("Wear OS"))
    }
}
