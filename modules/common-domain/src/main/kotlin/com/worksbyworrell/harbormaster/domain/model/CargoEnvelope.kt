package com.worksbyworrell.harbormaster.domain.model

import java.io.InputStream

data class CargoEnvelope(
    val payloadId: PayloadId,
    val carrierCode: CarrierCode,
    val berthId: BerthId,
    val originalFilename: String,
    val declaredByteSize: Long,
    val headers: Map<String, String> = emptyMap(),
    val streamSupplier: () -> InputStream,
) {
    init {
        require(originalFilename.isNotBlank()) { "originalFilename must not be blank" }
        require(declaredByteSize >= 0) { "declaredByteSize must not be negative" }
    }

    fun openStream(): InputStream = streamSupplier()
}
