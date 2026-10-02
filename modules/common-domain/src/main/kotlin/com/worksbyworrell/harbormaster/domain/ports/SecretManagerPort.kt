package com.worksbyworrell.harbormaster.domain.ports

import com.worksbyworrell.harbormaster.domain.model.BerthId

interface SecretManagerPort {
    suspend fun getBerthCredentials(berthId: BerthId): Map<String, String>

    suspend fun getStorageCredentials(storageName: String): Map<String, String>
}
