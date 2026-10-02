package com.worksbyworrell.harbormaster.domain

import com.worksbyworrell.harbormaster.domain.model.BerthId
import com.worksbyworrell.harbormaster.domain.model.CargoEnvelope
import com.worksbyworrell.harbormaster.domain.model.CarrierCode
import com.worksbyworrell.harbormaster.domain.model.DischargeRoute
import com.worksbyworrell.harbormaster.domain.model.PayloadId
import com.worksbyworrell.harbormaster.domain.model.RouteDestination
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class CargoEnvelopeAndRouteTest {
    @Nested
    inner class CargoEnvelopeTests {
        @Test
        fun `valid cargo envelope streams content`() {
            val bytes = "test data payload".toByteArray()
            val envelope =
                CargoEnvelope(
                    payloadId = PayloadId(),
                    carrierCode = CarrierCode("TITAN"),
                    berthId = BerthId("BERTH-1"),
                    originalFilename = "payload.dat",
                    declaredByteSize = bytes.size.toLong(),
                    headers = mapOf("X-Source" to "Test"),
                    streamSupplier = { ByteArrayInputStream(bytes) },
                )

            assertEquals("payload.dat", envelope.originalFilename)
            assertEquals(bytes.size.toLong(), envelope.declaredByteSize)
            assertEquals("Test", envelope.headers["X-Source"])

            val stream = envelope.openStream()
            val readBytes = stream.readBytes()
            assertEquals("test data payload", String(readBytes))
        }

        @Test
        fun `cargo envelope validation failures`() {
            assertThrows<IllegalArgumentException> {
                CargoEnvelope(
                    payloadId = PayloadId(),
                    carrierCode = CarrierCode("TITAN"),
                    berthId = BerthId("BERTH-1"),
                    originalFilename = "",
                    declaredByteSize = 10L,
                    streamSupplier = { ByteArrayInputStream(ByteArray(0)) },
                )
            }

            assertThrows<IllegalArgumentException> {
                CargoEnvelope(
                    payloadId = PayloadId(),
                    carrierCode = CarrierCode("TITAN"),
                    berthId = BerthId("BERTH-1"),
                    originalFilename = "file.txt",
                    declaredByteSize = -1L,
                    streamSupplier = { ByteArrayInputStream(ByteArray(0)) },
                )
            }
        }
    }

    @Nested
    inner class DischargeRouteTests {
        @Test
        fun `valid route destination and discharge route`() {
            val destination = RouteDestination("kafka://events.admitted")
            assertEquals("kafka://events.admitted", destination.value)

            val route =
                DischargeRoute(
                    destination = destination,
                    protocol = "kafka",
                    headers = mapOf("compression" to "snappy"),
                )
            assertEquals("kafka", route.protocol)
            assertEquals("snappy", route.headers["compression"])
        }

        @Test
        fun `discharge route validation failures`() {
            assertThrows<IllegalArgumentException> { RouteDestination("") }
            assertThrows<IllegalArgumentException> { RouteDestination("   ") }

            assertThrows<IllegalArgumentException> {
                DischargeRoute(
                    destination = RouteDestination("valid.target"),
                    protocol = "",
                )
            }
        }
    }
}
