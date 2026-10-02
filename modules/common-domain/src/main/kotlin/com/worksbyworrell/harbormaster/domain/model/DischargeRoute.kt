package com.worksbyworrell.harbormaster.domain.model

@JvmInline
value class RouteDestination(val value: String) {
    init {
        require(value.isNotBlank()) { "RouteDestination must not be blank" }
    }
}

data class DischargeRoute(
    val destination: RouteDestination,
    val protocol: String,
    val headers: Map<String, String> = emptyMap(),
) {
    init {
        require(protocol.isNotBlank()) { "protocol must not be blank" }
    }
}
