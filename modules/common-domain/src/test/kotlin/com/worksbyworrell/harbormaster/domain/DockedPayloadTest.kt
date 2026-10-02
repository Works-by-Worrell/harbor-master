package com.worksbyworrell.harbormaster.domain

import com.worksbyworrell.harbormaster.domain.exception.InvalidPayloadStateException
import com.worksbyworrell.harbormaster.domain.model.BerthId
import com.worksbyworrell.harbormaster.domain.model.CarrierCode
import com.worksbyworrell.harbormaster.domain.model.DockedPayload
import com.worksbyworrell.harbormaster.domain.model.PayloadId
import com.worksbyworrell.harbormaster.domain.model.PayloadStatus
import com.worksbyworrell.harbormaster.domain.model.QuarantineHash
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DockedPayloadTest {
    private val sampleCarrier = CarrierCode("CARRIER-01")
    private val sampleBerth = BerthId("BERTH-NORTH-1")
    private val sampleHash = QuarantineHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

    @Nested
    inner class ValueClassTests {
        @Test
        fun `PayloadId creates and formats correctly`() {
            val uuid = UUID.randomUUID()
            val id = PayloadId(uuid)
            assertEquals(uuid, id.value)
            assertEquals(uuid.toString(), id.toString())
        }

        @Test
        fun `QuarantineHash validation checks`() {
            val hash = QuarantineHash("sha256:abcd")
            assertEquals("sha256:abcd", hash.value)

            assertThrows<IllegalArgumentException> {
                QuarantineHash("")
            }
            assertThrows<IllegalArgumentException> {
                QuarantineHash("   ")
            }
        }
    }

    @Nested
    inner class ValidationTests {
        @Test
        fun `instantiation fails on blank filename`() {
            assertThrows<IllegalArgumentException> {
                DockedPayload(
                    carrierCode = sampleCarrier,
                    berthId = sampleBerth,
                    originalFilename = "  ",
                    declaredByteSize = 100L,
                )
            }
        }

        @Test
        fun `instantiation fails on negative declared byte size`() {
            assertThrows<IllegalArgumentException> {
                DockedPayload(
                    carrierCode = sampleCarrier,
                    berthId = sampleBerth,
                    originalFilename = "manifest.xml",
                    declaredByteSize = -1L,
                )
            }
        }

        @Test
        fun `instantiation fails on negative actual byte size`() {
            assertThrows<IllegalArgumentException> {
                DockedPayload(
                    carrierCode = sampleCarrier,
                    berthId = sampleBerth,
                    originalFilename = "manifest.xml",
                    declaredByteSize = 100L,
                    actualByteSize = -5L,
                )
            }
        }
    }

    @Nested
    inner class StateMachineHappyPath {
        @Test
        fun `full lifecycle from docked to discharged`() {
            val docked =
                DockedPayload(
                    carrierCode = sampleCarrier,
                    berthId = sampleBerth,
                    originalFilename = "manifest.xml",
                    declaredByteSize = 1024L,
                )
            assertEquals(PayloadStatus.DOCKED, docked.status)
            assertNotNull(docked.id)
            assertNotNull(docked.dockedAt)
            assertNull(docked.actualByteSize)
            assertNull(docked.quarantineHash)
            assertNull(docked.completedAt)
            assertNull(docked.rejectionReason)

            // Transition: DOCKED -> QUARANTINED
            val quarantined = docked.quarantine(1024L, sampleHash)
            assertEquals(PayloadStatus.QUARANTINED, quarantined.status)
            assertEquals(1024L, quarantined.actualByteSize)
            assertEquals(sampleHash, quarantined.quarantineHash)
            assertEquals(docked.id, quarantined.id)

            // Transition: QUARANTINED -> EXTRACTING
            val extracting = quarantined.startExtraction()
            assertEquals(PayloadStatus.EXTRACTING, extracting.status)

            // Transition: EXTRACTING -> VALIDATING
            val validating = extracting.startValidation()
            assertEquals(PayloadStatus.VALIDATING, validating.status)

            // Transition: VALIDATING -> ADMITTED
            val admittedTime = Instant.now()
            val admitted = validating.admit(admittedTime)
            assertEquals(PayloadStatus.ADMITTED, admitted.status)
            assertEquals(admittedTime, admitted.completedAt)

            // Transition: ADMITTED -> DISCHARGED
            val dischargedTime = Instant.now().plusSeconds(5)
            val discharged = admitted.discharge(dischargedTime)
            assertEquals(PayloadStatus.DISCHARGED, discharged.status)
            assertEquals(dischargedTime, discharged.completedAt)
        }

        @Test
        fun `rejection from various non-terminal states`() {
            val docked =
                DockedPayload(
                    carrierCode = sampleCarrier,
                    berthId = sampleBerth,
                    originalFilename = "cargo.tar.gz",
                    declaredByteSize = 2048L,
                )

            // Reject from DOCKED
            val rejectedFromDocked = docked.reject("Bad signature")
            assertEquals(PayloadStatus.REJECTED, rejectedFromDocked.status)
            assertEquals("Bad signature", rejectedFromDocked.rejectionReason)
            assertNotNull(rejectedFromDocked.completedAt)

            // Reject from QUARANTINED
            val quarantined = docked.quarantine(2048L, sampleHash)
            val rejectedFromQuarantined = quarantined.reject("Malware detected")
            assertEquals(PayloadStatus.REJECTED, rejectedFromQuarantined.status)
            assertEquals("Malware detected", rejectedFromQuarantined.rejectionReason)

            // Reject from EXTRACTING
            val extracting = quarantined.startExtraction()
            val rejectedFromExtracting = extracting.reject("Corrupted archive")
            assertEquals(PayloadStatus.REJECTED, rejectedFromExtracting.status)
            assertEquals("Corrupted archive", rejectedFromExtracting.rejectionReason)

            // Reject from VALIDATING
            val validating = extracting.startValidation()
            val rejectedFromValidating = validating.reject("Schema violation")
            assertEquals(PayloadStatus.REJECTED, rejectedFromValidating.status)
            assertEquals("Schema violation", rejectedFromValidating.rejectionReason)
        }
    }

    @Nested
    inner class InvalidStateTransitions {
        private val docked =
            DockedPayload(
                carrierCode = sampleCarrier,
                berthId = sampleBerth,
                originalFilename = "payload.bin",
                declaredByteSize = 512L,
            )
        private val quarantined = docked.quarantine(512L, sampleHash)
        private val extracting = quarantined.startExtraction()
        private val validating = extracting.startValidation()
        private val admitted = validating.admit()
        private val rejected = validating.reject("Invalid manifest")
        private val discharged = admitted.discharge()

        @Test
        fun `invalid transitions from DOCKED`() {
            assertThrows<InvalidPayloadStateException> { docked.startExtraction() }
            assertThrows<InvalidPayloadStateException> { docked.startValidation() }
            assertThrows<InvalidPayloadStateException> { docked.admit() }
            assertThrows<InvalidPayloadStateException> { docked.discharge() }
        }

        @Test
        fun `invalid transitions from QUARANTINED`() {
            assertThrows<InvalidPayloadStateException> { quarantined.quarantine(512L, sampleHash) }
            assertThrows<InvalidPayloadStateException> { quarantined.startValidation() }
            assertThrows<InvalidPayloadStateException> { quarantined.admit() }
            assertThrows<InvalidPayloadStateException> { quarantined.discharge() }
        }

        @Test
        fun `invalid transitions from EXTRACTING`() {
            assertThrows<InvalidPayloadStateException> { extracting.quarantine(512L, sampleHash) }
            assertThrows<InvalidPayloadStateException> { extracting.startExtraction() }
            assertThrows<InvalidPayloadStateException> { extracting.admit() }
            assertThrows<InvalidPayloadStateException> { extracting.discharge() }
        }

        @Test
        fun `invalid transitions from VALIDATING`() {
            assertThrows<InvalidPayloadStateException> { validating.quarantine(512L, sampleHash) }
            assertThrows<InvalidPayloadStateException> { validating.startExtraction() }
            assertThrows<InvalidPayloadStateException> { validating.startValidation() }
            assertThrows<InvalidPayloadStateException> { validating.discharge() }
        }

        @Test
        fun `invalid transitions from ADMITTED`() {
            assertThrows<InvalidPayloadStateException> { admitted.quarantine(512L, sampleHash) }
            assertThrows<InvalidPayloadStateException> { admitted.startExtraction() }
            assertThrows<InvalidPayloadStateException> { admitted.startValidation() }
            assertThrows<InvalidPayloadStateException> { admitted.admit() }
            assertThrows<InvalidPayloadStateException> { admitted.reject("Too late") }
        }

        @Test
        fun `invalid transitions from REJECTED`() {
            assertThrows<InvalidPayloadStateException> { rejected.quarantine(512L, sampleHash) }
            assertThrows<InvalidPayloadStateException> { rejected.startExtraction() }
            assertThrows<InvalidPayloadStateException> { rejected.startValidation() }
            assertThrows<InvalidPayloadStateException> { rejected.admit() }
            assertThrows<InvalidPayloadStateException> { rejected.reject("Already rejected") }
            assertThrows<InvalidPayloadStateException> { rejected.discharge() }
        }

        @Test
        fun `invalid transitions from DISCHARGED`() {
            assertThrows<InvalidPayloadStateException> { discharged.quarantine(512L, sampleHash) }
            assertThrows<InvalidPayloadStateException> { discharged.startExtraction() }
            assertThrows<InvalidPayloadStateException> { discharged.startValidation() }
            assertThrows<InvalidPayloadStateException> { discharged.admit() }
            assertThrows<InvalidPayloadStateException> { discharged.reject("Already discharged") }
            assertThrows<InvalidPayloadStateException> { discharged.discharge() }
        }

        @Test
        fun `reject requires non-blank reason`() {
            assertThrows<IllegalArgumentException> {
                docked.reject("")
            }
            assertThrows<IllegalArgumentException> {
                docked.reject("   ")
            }
        }
    }
}
