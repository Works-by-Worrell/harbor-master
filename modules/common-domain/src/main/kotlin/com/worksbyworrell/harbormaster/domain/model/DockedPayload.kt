package com.worksbyworrell.harbormaster.domain.model

import com.worksbyworrell.harbormaster.domain.exception.InvalidPayloadStateException
import java.time.Instant
import java.util.UUID

@JvmInline
value class PayloadId(val value: UUID = UUID.randomUUID()) {
    override fun toString(): String = value.toString()
}

@JvmInline
value class QuarantineHash(val value: String) {
    init {
        require(value.isNotBlank()) { "QuarantineHash must not be blank" }
    }
}

enum class PayloadStatus {
    DOCKED,
    QUARANTINED,
    EXTRACTING,
    VALIDATING,
    ADMITTED,
    REJECTED,
    DISCHARGED,
}

data class DockedPayload(
    val id: PayloadId = PayloadId(),
    val carrierCode: CarrierCode,
    val berthId: BerthId,
    val originalFilename: String,
    val declaredByteSize: Long,
    val actualByteSize: Long? = null,
    val quarantineHash: QuarantineHash? = null,
    val status: PayloadStatus = PayloadStatus.DOCKED,
    val dockedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val rejectionReason: String? = null,
) {
    init {
        require(originalFilename.isNotBlank()) { "originalFilename must not be blank" }
        require(declaredByteSize >= 0) { "declaredByteSize must not be negative" }
        if (actualByteSize != null) {
            require(actualByteSize >= 0) { "actualByteSize must not be negative" }
        }
    }

    fun quarantine(
        actualByteSize: Long,
        hash: QuarantineHash,
    ): DockedPayload {
        if (status != PayloadStatus.DOCKED) {
            throw InvalidPayloadStateException(
                "Cannot quarantine payload $id in status $status (must be DOCKED)",
            )
        }
        return copy(
            status = PayloadStatus.QUARANTINED,
            actualByteSize = actualByteSize,
            quarantineHash = hash,
        )
    }

    fun startExtraction(): DockedPayload {
        if (status != PayloadStatus.QUARANTINED) {
            throw InvalidPayloadStateException(
                "Cannot start extraction for payload $id in status $status (must be QUARANTINED)",
            )
        }
        return copy(status = PayloadStatus.EXTRACTING)
    }

    fun startValidation(): DockedPayload {
        if (status != PayloadStatus.EXTRACTING) {
            throw InvalidPayloadStateException(
                "Cannot start validation for payload $id in status $status (must be EXTRACTING)",
            )
        }
        return copy(status = PayloadStatus.VALIDATING)
    }

    fun admit(timestamp: Instant = Instant.now()): DockedPayload {
        if (status != PayloadStatus.VALIDATING) {
            throw InvalidPayloadStateException(
                "Cannot admit payload $id in status $status (must be VALIDATING)",
            )
        }
        return copy(
            status = PayloadStatus.ADMITTED,
            completedAt = timestamp,
        )
    }

    fun reject(
        reason: String,
        timestamp: Instant = Instant.now(),
    ): DockedPayload {
        require(reason.isNotBlank()) { "Rejection reason must not be blank" }
        val isTerminal =
            status == PayloadStatus.ADMITTED ||
                status == PayloadStatus.REJECTED ||
                status == PayloadStatus.DISCHARGED
        if (isTerminal) {
            throw InvalidPayloadStateException("Cannot reject payload $id in terminal status $status")
        }
        return copy(
            status = PayloadStatus.REJECTED,
            rejectionReason = reason,
            completedAt = timestamp,
        )
    }

    fun discharge(timestamp: Instant = Instant.now()): DockedPayload {
        if (status != PayloadStatus.ADMITTED) {
            throw InvalidPayloadStateException(
                "Cannot discharge payload $id in status $status (must be ADMITTED)",
            )
        }
        return copy(
            status = PayloadStatus.DISCHARGED,
            completedAt = timestamp,
        )
    }
}
