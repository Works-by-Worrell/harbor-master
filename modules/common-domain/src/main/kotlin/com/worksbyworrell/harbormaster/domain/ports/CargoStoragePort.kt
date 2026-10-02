package com.worksbyworrell.harbormaster.domain.ports

import com.worksbyworrell.harbormaster.domain.model.PayloadId
import com.worksbyworrell.harbormaster.domain.model.QuarantineHash
import java.io.InputStream

interface CargoStoragePort {
    suspend fun putQuarantineStream(
        payloadId: PayloadId,
        inputStream: InputStream,
        byteSize: Long,
    ): QuarantineHash

    suspend fun getQuarantineStream(payloadId: PayloadId): InputStream

    suspend fun promoteToAdmitted(payloadId: PayloadId): String

    suspend fun deleteQuarantined(payloadId: PayloadId)

    suspend fun exists(payloadId: PayloadId): Boolean
}
