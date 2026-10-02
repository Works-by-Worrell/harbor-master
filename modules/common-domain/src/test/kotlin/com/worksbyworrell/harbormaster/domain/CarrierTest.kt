package com.worksbyworrell.harbormaster.domain

import com.worksbyworrell.harbormaster.domain.model.BerthId
import com.worksbyworrell.harbormaster.domain.model.Carrier
import com.worksbyworrell.harbormaster.domain.model.CarrierCode
import com.worksbyworrell.harbormaster.domain.model.CarrierRateLimit
import com.worksbyworrell.harbormaster.domain.model.CarrierStatus
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarrierTest {
    private val berth1 = BerthId("BERTH-SFTP-01")
    private val berth2 = BerthId("BERTH-REST-01")
    private val berthUnassigned = BerthId("BERTH-UNASSIGNED")
    private val standardRateLimit = CarrierRateLimit(maxRequestsPerMinute = 60, maxBytesPerDay = 1_000_000_000L)

    @Nested
    inner class CarrierCodeTests {
        @Test
        fun `valid carrier code`() {
            val code = CarrierCode("TITAN_LOGISTICS")
            assertEquals("TITAN_LOGISTICS", code.value)
        }

        @Test
        fun `invalid carrier code fails on blank`() {
            assertThrows<IllegalArgumentException> { CarrierCode("") }
            assertThrows<IllegalArgumentException> { CarrierCode("   ") }
        }
    }

    @Nested
    inner class RateLimitTests {
        @Test
        fun `valid rate limit`() {
            val limit = CarrierRateLimit(maxRequestsPerMinute = 100, maxBytesPerDay = 500_000_000L)
            assertEquals(100, limit.maxRequestsPerMinute)
            assertEquals(500_000_000L, limit.maxBytesPerDay)
        }

        @Test
        fun `non-positive requests per minute fails`() {
            assertThrows<IllegalArgumentException> {
                CarrierRateLimit(maxRequestsPerMinute = 0, maxBytesPerDay = 100L)
            }
            assertThrows<IllegalArgumentException> {
                CarrierRateLimit(maxRequestsPerMinute = -1, maxBytesPerDay = 100L)
            }
        }

        @Test
        fun `non-positive bytes per day fails`() {
            assertThrows<IllegalArgumentException> {
                CarrierRateLimit(maxRequestsPerMinute = 10, maxBytesPerDay = 0L)
            }
            assertThrows<IllegalArgumentException> {
                CarrierRateLimit(maxRequestsPerMinute = 10, maxBytesPerDay = -100L)
            }
        }
    }

    @Nested
    inner class CarrierPermissionsTests {
        @Test
        fun `active carrier can access allowed berths`() {
            val carrier =
                Carrier(
                    code = CarrierCode("TITAN"),
                    name = "Titan Logistics",
                    status = CarrierStatus.ACTIVE,
                    rateLimit = standardRateLimit,
                    allowedBerths = setOf(berth1, berth2),
                )

            assertTrue(carrier.isActive)
            assertTrue(carrier.isBerthAllowed(berth1))
            assertTrue(carrier.isBerthAllowed(berth2))
            assertFalse(carrier.isBerthAllowed(berthUnassigned))
        }

        @Test
        fun `suspended carrier cannot access any berth`() {
            val carrier =
                Carrier(
                    code = CarrierCode("TITAN"),
                    name = "Titan Logistics",
                    status = CarrierStatus.SUSPENDED,
                    rateLimit = standardRateLimit,
                    allowedBerths = setOf(berth1, berth2),
                )

            assertFalse(carrier.isActive)
            assertFalse(carrier.isBerthAllowed(berth1))
            assertFalse(carrier.isBerthAllowed(berth2))
            assertFalse(carrier.isBerthAllowed(berthUnassigned))
        }

        @Test
        fun `provisioning carrier cannot access any berth`() {
            val carrier =
                Carrier(
                    code = CarrierCode("TITAN"),
                    name = "Titan Logistics",
                    status = CarrierStatus.PROVISIONING,
                    rateLimit = standardRateLimit,
                    allowedBerths = setOf(berth1),
                )

            assertFalse(carrier.isActive)
            assertFalse(carrier.isBerthAllowed(berth1))
        }
    }
}
