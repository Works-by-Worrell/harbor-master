package com.worksbyworrell.harbormaster.domain.exception

sealed class HarborException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String,
) : RuntimeException(message, cause)

class QuarantineViolationException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "QUARANTINE_VIOLATION")

class PayloadCorruptedException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "PAYLOAD_CORRUPTED")

class UnregisteredCarrierException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "UNREGISTERED_CARRIER")

class RateLimitExceededException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "RATE_LIMIT_EXCEEDED")

class BerthUnavailableException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "BERTH_UNAVAILABLE")

class StorageException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "STORAGE_ERROR")

class InvalidPayloadStateException(
    message: String,
    cause: Throwable? = null,
) : HarborException(message, cause, errorCode = "INVALID_PAYLOAD_STATE")
