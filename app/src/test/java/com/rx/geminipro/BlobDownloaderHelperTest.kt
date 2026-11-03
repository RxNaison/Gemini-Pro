package com.rx.geminipro

import com.rx.geminipro.utils.network.BlobDownloaderInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class BlobDownloaderHelperTest {

    @Test
    fun cleanBase64Payload_withDataUriPrefix_stripsPrefix() {
        val payload = "data:video/mp4;base64,AAAAIGZ0eXBpc29t"
        val cleaned = BlobDownloaderInterface.cleanBase64Payload(payload)
        assertEquals("AAAAIGZ0eXBpc29t", cleaned)
    }

    @Test
    fun cleanBase64Payload_withAudioDataUriPrefix_stripsPrefix() {
        val payload = "data:audio/mpeg;base64,SUQzBAAAAAAA"
        val cleaned = BlobDownloaderInterface.cleanBase64Payload(payload)
        assertEquals("SUQzBAAAAAAA", cleaned)
    }

    @Test
    fun cleanBase64Payload_withoutPrefix_returnsOriginal() {
        val payload = "AAAAIGZ0eXBpc29t"
        val cleaned = BlobDownloaderInterface.cleanBase64Payload(payload)
        assertEquals("AAAAIGZ0eXBpc29t", cleaned)
    }

    @Test
    fun cleanBase64Payload_emptyString_returnsEmpty() {
        val cleaned = BlobDownloaderInterface.cleanBase64Payload("")
        assertEquals("", cleaned)
    }
}
