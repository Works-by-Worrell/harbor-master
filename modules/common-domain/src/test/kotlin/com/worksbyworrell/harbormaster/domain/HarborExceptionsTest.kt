package com.worksbyworrell.harbormaster.domain

import com.worksbyworrell.harbormaster.domain.exception.BerthUnavailableException
import com.worksbyworrell.harbormaster.domain.exception.HarborException
import com.worksbyworrell.harbormaster.domain.exception.InvalidPayloadStateException
import com.worksbyworrell.harbormaster.domain.exception.PayloadCorruptedException
import com.worksbyworrell.harbormaster.domain.exception.QuarantineViolationException
import com.worksbyworrell.harbormaster.domain.exception.RateLimitExceededException
import com.worksbyworrell.harbormaster.domain.exception.StorageException
import com.worksbyworrell.harbormaster.domain.exception.UnregisteredCarrierException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HarborExceptionsTest {
    @Test
    fun `QuarantineViolationException properties`() {
        val cause = RuntimeException("Signature check failed")
        val ex = QuarantineViolationException("Payload quarantine violation", cause)
        assertEquals("Payload quarantine violation", ex.message)
        assertEquals("QUARANTINE_VIOLATION", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = QuarantineViolationException("Violation without cause")
        assertNull(noCause.cause)
    }

    @Test
    fun `PayloadCorruptedException properties`() {
        val cause = IllegalStateException("Corrupt magic bytes")
        val ex = PayloadCorruptedException("Payload corrupt", cause)
        assertEquals("Payload corrupt", ex.message)
        assertEquals("PAYLOAD_CORRUPTED", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = PayloadCorruptedException("Corrupted")
        assertNull(noCause.cause)
    }

    @Test
    fun `UnregisteredCarrierException properties`() {
        val cause = IllegalArgumentException("Carrier not in DB")
        val ex = UnregisteredCarrierException("Carrier UNKNOWN is not registered", cause)
        assertEquals("Carrier UNKNOWN is not registered", ex.message)
        assertEquals("UNREGISTERED_CARRIER", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = UnregisteredCarrierException("Unregistered")
        assertNull(noCause.cause)
    }

    @Test
    fun `RateLimitExceededException properties`() {
        val cause = RuntimeException("Redis token bucket empty")
        val ex = RateLimitExceededException("Rate limit exceeded for CARRIER-01", cause)
        assertEquals("Rate limit exceeded for CARRIER-01", ex.message)
        assertEquals("RATE_LIMIT_EXCEEDED", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = RateLimitExceededException("Rate limit hit")
        assertNull(noCause.cause)
    }

    @Test
    fun `BerthUnavailableException properties`() {
        val cause = RuntimeException("SFTP connection refused")
        val ex = BerthUnavailableException("Berth BERTH-1 is offline", cause)
        assertEquals("Berth BERTH-1 is offline", ex.message)
        assertEquals("BERTH_UNAVAILABLE", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = BerthUnavailableException("Berth down")
        assertNull(noCause.cause)
    }

    @Test
    fun `StorageException properties`() {
        val cause = RuntimeException("S3 500 internal error")
        val ex = StorageException("Failed to persist payload stream", cause)
        assertEquals("Failed to persist payload stream", ex.message)
        assertEquals("STORAGE_ERROR", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = StorageException("S3 error")
        assertNull(noCause.cause)
    }

    @Test
    fun `InvalidPayloadStateException properties`() {
        val cause = IllegalStateException("State check failed")
        val ex = InvalidPayloadStateException("Payload cannot be admitted from status DOCKED", cause)
        assertEquals("Payload cannot be admitted from status DOCKED", ex.message)
        assertEquals("INVALID_PAYLOAD_STATE", ex.errorCode)
        assertEquals(cause, ex.cause)
        assertTrue((ex as Any) is HarborException)

        val noCause = InvalidPayloadStateException("Invalid state")
        assertNull(noCause.cause)
    }
}
