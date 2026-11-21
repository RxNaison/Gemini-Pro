package com.rx.geminipro

import com.rx.geminipro.viewmodels.ClipboardContentType
import com.rx.geminipro.viewmodels.GeminiUiState
import com.rx.geminipro.viewmodels.toggleAudioSelectionMode
import com.rx.geminipro.viewmodels.toggleVideoSelectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiContractTest {

    @Test
    fun defaultUiState_hasExpectedInitialValues() {
        val state = GeminiUiState()

        assertFalse(state.isApplicationReady)
        assertFalse(state.isKeepScreenOn)
        assertFalse(state.isMenuLeft)
        assertFalse(state.isKeyboardVisible)
        assertEquals(ClipboardContentType.NONE, state.clipboardContentType)
        assertNull(state.activeWebViewUrl)
        assertFalse(state.canWebViewGoBack)
        assertFalse(state.isReloading)
        assertEquals(0, state.loadingProgress)
        assertFalse(state.isVideoSelectionMode)
        assertFalse(state.isAudioSelectionMode)
        assertNull(state.updateInfo)
    }

    @Test
    fun toggleVideoSelectionMode_whenIdle_enablesVideoOnly() {
        val updated = GeminiUiState().toggleVideoSelectionMode()

        assertTrue(updated.isVideoSelectionMode)
        assertFalse(updated.isAudioSelectionMode)
    }

    @Test
    fun toggleVideoSelectionMode_whileAudioActive_switchesToVideo() {
        val updated = GeminiUiState(isAudioSelectionMode = true).toggleVideoSelectionMode()

        assertTrue(updated.isVideoSelectionMode)
        assertFalse(updated.isAudioSelectionMode)
    }

    @Test
    fun toggleVideoSelectionMode_twice_turnsVideoOff() {
        val updated = GeminiUiState().toggleVideoSelectionMode().toggleVideoSelectionMode()

        assertFalse(updated.isVideoSelectionMode)
        assertFalse(updated.isAudioSelectionMode)
    }

    @Test
    fun toggleAudioSelectionMode_whenIdle_enablesAudioOnly() {
        val updated = GeminiUiState().toggleAudioSelectionMode()

        assertTrue(updated.isAudioSelectionMode)
        assertFalse(updated.isVideoSelectionMode)
    }

    @Test
    fun toggleAudioSelectionMode_whileVideoActive_switchesToAudio() {
        val updated = GeminiUiState(isVideoSelectionMode = true).toggleAudioSelectionMode()

        assertTrue(updated.isAudioSelectionMode)
        assertFalse(updated.isVideoSelectionMode)
    }

    @Test
    fun toggleAudioSelectionMode_twice_turnsAudioOff() {
        val updated = GeminiUiState().toggleAudioSelectionMode().toggleAudioSelectionMode()

        assertFalse(updated.isAudioSelectionMode)
        assertFalse(updated.isVideoSelectionMode)
    }

    @Test
    fun toggleSelectionModes_leaveOtherStateUnchanged() {
        val state = GeminiUiState(
            isKeepScreenOn = true,
            activeWebViewUrl = "https://aistudio.google.com",
            loadingProgress = 65,
        )

        listOf(state.toggleVideoSelectionMode(), state.toggleAudioSelectionMode()).forEach { updated ->
            assertEquals(
                state,
                updated.copy(isVideoSelectionMode = false, isAudioSelectionMode = false),
            )
        }
    }
}
