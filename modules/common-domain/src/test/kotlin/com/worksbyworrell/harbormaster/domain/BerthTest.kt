package com.worksbyworrell.harbormaster.domain

import com.worksbyworrell.harbormaster.domain.model.Berth
import com.worksbyworrell.harbormaster.domain.model.BerthId
import com.worksbyworrell.harbormaster.domain.model.BerthType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BerthTest {
    @Nested
    inner class BerthIdTests {
        @Test
        fun `valid berth id`() {
            val id = BerthId("SFTP_INBOUND_01")
            assertEquals("SFTP_INBOUND_01", id.value)
        }

        @Test
        fun `invalid berth id on blank string`() {
            assertThrows<IllegalArgumentException> { BerthId("") }
            assertThrows<IllegalArgumentException> { BerthId("   ") }
        }
    }

    @Nested
    inner class BerthCreationTests {
        @Test
        fun `create berth across different types`() {
            val sftpPull =
                Berth(
                    id = BerthId("PULL-1"),
                    name = "SFTP Pull Vendor",
                    type = BerthType.SFTP_PULL,
                    active = true,
                    config = mapOf("host" to "sftp.vendor.com", "port" to "22"),
                )
            assertEquals(BerthType.SFTP_PULL, sftpPull.type)
            assertTrue(sftpPull.active)
            assertEquals("sftp.vendor.com", sftpPull.config["host"])

            val sftpPush =
                Berth(
                    id = BerthId("PUSH-1"),
                    name = "SFTP Push Drop",
                    type = BerthType.SFTP_PUSH,
                )
            assertEquals(BerthType.SFTP_PUSH, sftpPush.type)
            assertTrue(sftpPush.active)
            assertTrue(sftpPush.config.isEmpty())

            val restDirect =
                Berth(
                    id = BerthId("REST-1"),
                    name = "REST Inbound Endpoint",
                    type = BerthType.REST_DIRECT,
                )
            assertEquals(BerthType.REST_DIRECT, restDirect.type)

            val bucketInbox =
                Berth(
                    id = BerthId("BUCKET-1"),
                    name = "S3 Bucket Inbox",
                    type = BerthType.BUCKET_INBOX,
                )
            assertEquals(BerthType.BUCKET_INBOX, bucketInbox.type)
        }
    }
}
