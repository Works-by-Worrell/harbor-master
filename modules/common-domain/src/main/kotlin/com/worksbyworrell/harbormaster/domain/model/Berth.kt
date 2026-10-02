package com.worksbyworrell.harbormaster.domain.model

@JvmInline
value class BerthId(val value: String) {
    init {
        require(value.isNotBlank()) { "BerthId must not be blank" }
    }
}

enum class BerthType {
    SFTP_PULL,
    SFTP_PUSH,
    REST_DIRECT,
    BUCKET_INBOX,
}

data class Berth(
    val id: BerthId,
    val name: String,
    val type: BerthType,
    val active: Boolean = true,
    val config: Map<String, String> = emptyMap(),
)
