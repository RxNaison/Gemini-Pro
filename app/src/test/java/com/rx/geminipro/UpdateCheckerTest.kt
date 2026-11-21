package com.rx.geminipro

import com.rx.geminipro.utils.system.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun isNewer_newerMinorVersion_returnsTrue() {
        assertTrue(UpdateChecker.isNewer("1.7.6", "1.7.5"))
    }

    @Test
    fun isNewer_newerMajorVersion_returnsTrue() {
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.7.5"))
    }

    @Test
    fun isNewer_sameVersion_returnsFalse() {
        assertFalse(UpdateChecker.isNewer("1.7.5", "1.7.5"))
    }

    @Test
    fun isNewer_olderVersion_returnsFalse() {
        assertFalse(UpdateChecker.isNewer("1.7.4", "1.7.5"))
    }

    @Test
    fun isNewer_patchVersion_returnsTrue() {
        assertTrue(UpdateChecker.isNewer("1.7.5.1", "1.7.5"))
    }

    @Test
    fun isNewer_lessSegmentsEqual_returnsFalse() {
        assertFalse(UpdateChecker.isNewer("1.7", "1.7.0"))
    }

    @Test
    fun getReleaseUrl_returnsValidUrl() {
        val url = UpdateChecker.getReleaseUrl()
        assertEquals("https://github.com/RxNaison/Gemini-Pro/releases/latest", url)
    }
}
