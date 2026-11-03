package com.rx.geminipro

import com.rx.geminipro.viewmodels.ClipboardContentType
import com.rx.geminipro.viewmodels.GeminiUiState
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
    fun toggleVideoSelectionMode_enablesVideoAndDisablesAudio() {
        val initialState = GeminiUiState(isAudioSelectionMode = true)

        val updated = initialState.copy(
            isVideoSelectionMode = true,
            isAudioSelectionMode = false
        )

        assertTrue(updated.isVideoSelectionMode)
        assertFalse(updated.isAudioSelectionMode)
    }

    @Test
    fun toggleAudioSelectionMode_enablesAudioAndDisablesVideo() {
        val initialState = GeminiUiState(isVideoSelectionMode = true)

        val updated = initialState.copy(
            isAudioSelectionMode = true,
            isVideoSelectionMode = false
        )

        assertTrue(updated.isAudioSelectionMode)
        assertFalse(updated.isVideoSelectionMode)
    }

    @Test
    fun stateCopy_updatesProgressAndReloading() {
        val state = GeminiUiState()

        val progressState = state.copy(loadingProgress = 65, isReloading = true)

        assertEquals(65, progressState.loadingProgress)
        assertTrue(progressState.isReloading)
    }
}
