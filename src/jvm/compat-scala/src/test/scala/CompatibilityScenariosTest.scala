import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.eventdriven.strictland.{Json, MessageContract}
import org.scalatest.funsuite.AnyFunSuite

import java.util.UUID

case class OrderPlaced(orderId: UUID, customer: String)

case class OrderPlacedWithCoupon(orderId: UUID, customer: String, couponCode: Option[String])

case class OrderPlacedWithCouponAndNote(
    orderId: UUID,
    customer: String,
    couponCode: Option[String],
    note: Option[String],
    channel: String
)

case class OrderPlacedWithCouponAndNoteV1(
    orderId: UUID,
    customer: String,
    couponCode: Option[String],
    note: Option[String]
)

case class OrderPlacedWithCurrency(orderId: UUID, customer: String, currency: String)

case class Address(street: String, postalCode: String)

case class Customer(name: String, address: Address)

case class OrderShipped(orderId: UUID, customer: Customer)

case class NumericAddress(street: String, postalCode: Int)

case class NumericCustomer(name: String, address: NumericAddress)

case class OrderShippedWithNumericPostalCode(orderId: UUID, customer: NumericCustomer)

case class CartCheckedOut(cartId: UUID, tags: List[String])

case class CartCheckedOutWithTagSet(cartId: UUID, tags: Set[String])

case class ProfileUpdated(profileId: UUID, attributes: Map[String, String])

case class ProfileUpdatedWithNickname(profileId: UUID, attributes: Map[String, String], nickname: Option[String])

class CompatibilityScenariosTest extends AnyFunSuite {
  private val FixedId = UUID.fromString("00000000-0000-0000-0000-000000000001")

  private def contract() =
    MessageContract.specification(
      Json.Jackson.of(
        JsonMapper
          .builder()
          .addModule(DefaultScalaModule)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .build()
      )
    )

  test("given a case class, when the target adds an Option field, then backward compatible") {
    contract()
      .`given`(OrderPlaced(FixedId, "Alice"))
      .whenDeserializedAs(classOf[OrderPlacedWithCoupon])
      .thenBackwardCompatible(order => assert(order.couponCode.isEmpty))
  }

  test("given a case class, when the target adds a required field, then not backward compatible") {
    val error = intercept[AssertionError] {
      contract()
        .`given`(OrderPlaced(FixedId, "Alice"))
        .whenDeserializedAs(classOf[OrderPlacedWithCurrency])
        .thenBackwardCompatible()
    }

    assert(String.valueOf(error.getMessage).contains("currency"))
  }

  test("given a case class with an Option value, when the target drops it, then forward compatible") {
    contract()
      .`given`(OrderPlacedWithCoupon(FixedId, "Alice", Some("SAVE10")))
      .whenDeserializedAs(classOf[OrderPlaced])
      .thenForwardCompatible(order => assert(order.customer == "Alice"))
  }

  test("given Option values, when the target drops another field, then forward compatible") {
    contract()
      .`given`(OrderPlacedWithCouponAndNote(FixedId, "Alice", Some("SAVE10"), None, "web"))
      .whenDeserializedAs(classOf[OrderPlacedWithCouponAndNoteV1])
      .thenForwardCompatible(order => assert(order.couponCode.contains("SAVE10") && order.note.isEmpty))
  }

  test("given a nested case class, when the target changes an inner field type, then fails with its path") {
    val error = intercept[AssertionError] {
      contract()
        .`given`(OrderShipped(FixedId, Customer("Alice", Address("Main St", "00501"))))
        .whenDeserializedAs(classOf[OrderShippedWithNumericPostalCode])
        .thenBackwardCompatible()
    }

    assert(String.valueOf(error.getMessage).contains("customer.address.postalCode"))
  }

  test("given a list, when the target reads it as a set, then backward compatible") {
    contract()
      .`given`(CartCheckedOut(FixedId, List("gift", "express")))
      .whenDeserializedAs(classOf[CartCheckedOutWithTagSet])
      .thenBackwardCompatible(cart => assert(cart.tags == Set("gift", "express")))
  }

  test("given a map, when the target adds an Option field, then backward compatible") {
    contract()
      .`given`(ProfileUpdated(FixedId, Map("zip" -> "00501", "city" -> "Holtsville")))
      .whenDeserializedAs(classOf[ProfileUpdatedWithNickname])
      .thenBackwardCompatible(profile => assert(profile.attributes("zip") == "00501" && profile.nickname.isEmpty))
  }
}
