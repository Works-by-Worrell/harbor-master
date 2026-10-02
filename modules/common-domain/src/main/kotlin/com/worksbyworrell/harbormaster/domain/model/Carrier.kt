package com.worksbyworrell.harbormaster.domain.model

@JvmInline
value class CarrierCode(val value: String) {
    init {
        require(value.isNotBlank()) { "CarrierCode must not be blank" }
    }
}

enum class CarrierStatus {
    ACTIVE,
    SUSPENDED,
    PROVISIONING,
}

data class CarrierRateLimit(
    val maxRequestsPerMinute: Int,
    val maxBytesPerDay: Long,
) {
    init {
        require(maxRequestsPerMinute > 0) { "maxRequestsPerMinute must be positive" }
        require(maxBytesPerDay > 0) { "maxBytesPerDay must be positive" }
    }
}

data class Carrier(
    val code: CarrierCode,
    val name: String,
    val status: CarrierStatus,
    val rateLimit: CarrierRateLimit,
    val allowedBerths: Set<BerthId>,
) {
    val isActive: Boolean
        get() = status == CarrierStatus.ACTIVE

    fun isBerthAllowed(berthId: BerthId): Boolean = isActive && allowedBerths.contains(berthId)
}
