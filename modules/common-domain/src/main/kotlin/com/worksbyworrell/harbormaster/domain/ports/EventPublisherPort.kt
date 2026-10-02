package com.worksbyworrell.harbormaster.domain.ports

import com.worksbyworrell.harbormaster.domain.model.DockedPayload

interface EventPublisherPort {
    suspend fun publishPayloadDocked(payload: DockedPayload)

    suspend fun publishPayloadExtracted(payload: DockedPayload)

    suspend fun publishPayloadAdmitted(payload: DockedPayload)

    suspend fun publishPayloadRejected(payload: DockedPayload)
}
