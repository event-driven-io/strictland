import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.eventdriven.strictland.Json
import io.eventdriven.strictland.MessageContract
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val FIXED_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

private fun contract() =
    MessageContract.specification(
        Json.Jackson.of(
            jacksonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build()
        )
    )

data class OrderPlaced(val orderId: UUID, val customer: String)

data class OrderPlacedWithCoupon(val orderId: UUID, val customer: String, val couponCode: String?)

data class OrderPlacedWithCurrency(val orderId: UUID, val customer: String, val currency: String)

data class Address(val street: String, val postalCode: String)

data class Customer(val name: String, val address: Address)

data class OrderShipped(val orderId: UUID, val customer: Customer)

data class NumericAddress(val street: String, val postalCode: Int)

data class NumericCustomer(val name: String, val address: NumericAddress)

data class OrderShippedWithNumericPostalCode(val orderId: UUID, val customer: NumericCustomer)

data class CartCheckedOut(val cartId: UUID, val tags: List<String>)

data class CartCheckedOutWithTagSet(val cartId: UUID, val tags: Set<String>)

class CompatibilityScenariosTest {

    @Test
    fun `given a data class, when the target adds a nullable field, then backward compatible`() {
        contract()
            .given(OrderPlaced(FIXED_ID, "Alice"))
            .whenDeserializedAs(OrderPlacedWithCoupon::class.java)
            .thenBackwardCompatible { assertNull(it.couponCode) }
    }

    @Test
    fun `given a data class, when the target adds a non-null field, then not backward compatible`() {
        val error =
            assertFailsWith<Throwable> {
                contract()
                    .given(OrderPlaced(FIXED_ID, "Alice"))
                    .whenDeserializedAs(OrderPlacedWithCurrency::class.java)
                    .thenBackwardCompatible()
            }

        assertContains(
            generateSequence(error) { it.cause }.joinToString { it.message.orEmpty() },
            "currency",
        )
    }

    @Test
    fun `given a data class, when the target drops a field, then forward compatible`() {
        contract()
            .given(OrderPlacedWithCoupon(FIXED_ID, "Alice", "SAVE10"))
            .whenDeserializedAs(OrderPlaced::class.java)
            .thenForwardCompatible { assertEquals("Alice", it.customer) }
    }

    @Test
    fun `given a nested data class, when the target changes an inner field type, then fails with its path`() {
        val error =
            assertFailsWith<AssertionError> {
                contract()
                    .given(OrderShipped(FIXED_ID, Customer("Alice", Address("Main St", "00501"))))
                    .whenDeserializedAs(OrderShippedWithNumericPostalCode::class.java)
                    .thenBackwardCompatible()
            }

        assertContains(error.message.orEmpty(), "customer.address.postalCode")
    }

    @Test
    fun `given a list, when the target reads it as a set, then backward compatible`() {
        contract()
            .given(CartCheckedOut(FIXED_ID, listOf("gift", "express")))
            .whenDeserializedAs(CartCheckedOutWithTagSet::class.java)
            .thenBackwardCompatible { assertEquals(setOf("gift", "express"), it.tags) }
    }
}
