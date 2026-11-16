package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
final class BackwardCompatibilityTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Nested
    final class AddingAnOptionalField {
        private record OrderPlaced(UUID orderId, String customer) {}

        private record OrderPlacedWithCoupon(
                UUID orderId, String customer, @Nullable String couponCode) {}

        @Test
        void givenOrderWithoutCoupon_whenReadByTypeThatAddedOptionalCoupon_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new OrderPlaced(FIXED_ID, "Alice"))
                    .whenDeserializedAs(OrderPlacedWithCoupon.class)
                    .thenBackwardCompatible(order -> assertNull(order.couponCode()));
        }
    }

    @Nested
    final class AddingARequiredField {
        private record OrderConfirmed(UUID orderId, String customer) {}

        private record OrderConfirmedWithCurrency(UUID orderId, String customer, String currency) {}

        @Test
        void givenOrderWithoutCurrency_whenReadByTypeThatAddedRequiredCurrency_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new OrderConfirmed(FIXED_ID, "Alice"))
                            .whenDeserializedAs(OrderConfirmedWithCurrency.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Required field") && message.contains("currency"));
        }
    }

    @Nested
    final class RenamingAField {
        private record OrderCompleted(UUID orderId, String customer) {}

        private record OrderCompletedRenamed(UUID orderId, String client) {}

        @Test
        void givenOrderWithCustomer_whenReadByTypeThatRenamedItToClient_thenNotBackwardCompatible() {
            assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new OrderCompleted(FIXED_ID, "Alice"))
                            .whenDeserializedAs(OrderCompletedRenamed.class)
                            .thenBackwardCompatible());
        }
    }

    @Nested
    final class TighteningAnOptionalFieldToRequired {
        private record OrderDrafted(UUID orderId, @Nullable String customer) {}

        private record OrderDraftedRequiredCustomer(UUID orderId, String customer) {}

        @Test
        void givenOrderWithNullCustomer_whenReadByTypeThatMadeCustomerRequired_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new OrderDrafted(FIXED_ID, null))
                            .whenDeserializedAs(OrderDraftedRequiredCustomer.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("customer"));
        }
    }

    @Nested
    final class ReorderingFields {
        private record OrderShipped(UUID orderId, String customer) {}

        private record OrderShippedReordered(String customer, UUID orderId) {}

        @Test
        void givenOrder_whenReadByTypeWithTheSameFieldsInADifferentOrder_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new OrderShipped(FIXED_ID, "Alice"))
                    .whenDeserializedAs(OrderShippedReordered.class)
                    .thenBackwardCompatible();
        }
    }

    @Nested
    final class ReadingAStoredEventByConvention {
        private record CustomerRegisteredV1(UUID customerId, String name) {}

        private record CustomerRegisteredV2(
                UUID customerId, String name, @Nullable String referralCode) {}

        private record AccountOpenedV1(UUID accountId, String owner) {}

        private record AccountOpenedV2(
                UUID accountId, String owner, @Nullable String currency) {}

        @Test
        void givenAStoredEventNamedAfterItsClass_whenReadByTheEvolvedType_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(MessageSnapshot.of(CustomerRegisteredV1.class))
                    .whenDeserializedAs(CustomerRegisteredV2.class)
                    .thenBackwardCompatible(event -> assertNull(event.referralCode()));
        }

        @Test
        void givenAStoredEventNamedByItsMessageType_whenReadByTheEvolvedType_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(MessageSnapshot.ofTypeNamed("AccountOpenedV1"))
                    .whenDeserializedAs(AccountOpenedV2.class)
                    .thenBackwardCompatible(event -> assertNull(event.currency()));
        }
    }

    @Nested
    final class ReadingAStoredOldEvent {
        private record ShoppingCartConfirmed(UUID shoppingCartId, String clientId, OffsetDateTime confirmedAt) {}

        @Test
        void givenAStoredOldEvent_whenReadByTheCurrentType_thenBackwardCompatible() {
            var path = Path.of("ReadingAStoredOldEvent/ShoppingCartConfirmedV1.reference.snap.approved.json");

            MessageContract.specification(Json.Jackson.defaults())
                    .given(MessageSnapshot.at(path))
                    .whenDeserializedAs(ShoppingCartConfirmed.class)
                    .thenBackwardCompatible();
        }
    }

    @Nested
    final class ComparingASnapshotWithItsOriginalType {
        private record InvoiceRaisedV1(UUID invoiceId, long raisedAt) {}

        private record InvoiceRaised(UUID invoiceId, OffsetDateTime raisedAt) {}

        private static final MessageTypeMapper RESOLVING_INVOICE_RAISED_V1 = new MessageTypeMapper() {
            @Override
            public MessageTypeName name(Class<?> type) {
                return MessageTypeName.of(type.getSimpleName());
            }

            @Override
            public Optional<Class<?>> type(String name) {
                return Optional.of(InvoiceRaisedV1.class);
            }
        };

        @Test
        void givenASnapshotReferencedByClass_whenTargetReadsAFieldAsADifferentValue_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(MessageSnapshot.of(InvoiceRaisedV1.class))
                            .whenDeserializedAs(InvoiceRaised.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Field 'raisedAt' value differs"), message);
        }

        @Test
        void
                givenASnapshotReferencedByAResolvableMessageType_whenTargetReadsAFieldAsADifferentValue_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(
                                    Json.Jackson.defaults().messageTypeMapper(RESOLVING_INVOICE_RAISED_V1))
                            .given(MessageSnapshot.ofTypeNamed("InvoiceRaisedV1"))
                            .whenDeserializedAs(InvoiceRaised.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Field 'raisedAt' value differs"), message);
        }

        @Test
        void givenASnapshotReferencedByClass_whenReadByItsOwnType_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(MessageSnapshot.of(InvoiceRaisedV1.class))
                    .whenDeserializedAs(InvoiceRaisedV1.class)
                    .thenBackwardCompatible(invoice -> assertEquals(1704110400000L, invoice.raisedAt()));
        }
    }

    @Nested
    final class WhenTheSerializerDateFormatChanged {
        private record InvoiceIssued(UUID invoiceId, OffsetDateTime confirmedAt) {}

        @Test
        void givenAnOldEpochTimestampEvent_whenReadByTheCurrentIsoType_thenNotBackwardCompatible() {
            var path = Path.of(
                    "WhenTheSerializerDateFormatChanged/InvoiceIssuedV1_EpochTimestamp.reference.snap.approved.json");

            var issuedAt = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

            assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(MessageSnapshot.at(path))
                            .whenDeserializedAs(InvoiceIssued.class)
                            .thenBackwardCompatible(invoice -> assertEquals(issuedAt, invoice.confirmedAt())));
        }
    }

    @Nested
    final class WideningAFieldType {
        private record PaymentCaptured(UUID paymentId, int amount) {}

        private record PaymentCapturedWiderAmount(UUID paymentId, long amount) {}

        @Test
        void givenAnIntAmount_whenReadByTypeThatWidenedItToLong_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new PaymentCaptured(FIXED_ID, 100))
                    .whenDeserializedAs(PaymentCapturedWiderAmount.class)
                    .thenBackwardCompatible();
        }
    }

    @Nested
    final class EvolvingANestedValueObject {
        private record Address(String street, String zipCode) {}

        private record AddressWithCountry(
                String street, String zipCode, @Nullable String country) {}

        private record AddressWithNumericZipCode(String street, int zipCode) {}

        private record CustomerRelocated(UUID customerId, Address address) {}

        private record CustomerRelocatedWithCountry(UUID customerId, AddressWithCountry address) {}

        private record CustomerRelocatedWithNumericZipCode(UUID customerId, AddressWithNumericZipCode address) {}

        @Test
        void givenANestedRecord_whenTargetAddsAnOptionalFieldToIt_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new CustomerRelocated(FIXED_ID, new Address("Main St 1", "01234")))
                    .whenDeserializedAs(CustomerRelocatedWithCountry.class)
                    .thenBackwardCompatible();
        }

        @Test
        void givenANestedRecord_whenTargetChangesTheTypeOfItsField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new CustomerRelocated(FIXED_ID, new Address("Main St 1", "01234")))
                            .whenDeserializedAs(CustomerRelocatedWithNumericZipCode.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Field 'address.zipCode' value differs"), message);
        }
    }

    @Nested
    final class EvolvingAListOfValueObjects {
        private record OrderLine(String productId, int quantity) {}

        private record OrderLineWithDiscount(
                String productId, int quantity, @Nullable String discountCode) {}

        private record OrderSubmitted(UUID orderId, List<OrderLine> lines) {}

        private record OrderSubmittedWithDiscounts(UUID orderId, List<OrderLineWithDiscount> lines) {}

        @Test
        void givenAListOfRecords_whenTargetAddsAnOptionalFieldToEachElement_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new OrderSubmitted(FIXED_ID, List.of(new OrderLine("book", 2), new OrderLine("pen", 1))))
                    .whenDeserializedAs(OrderSubmittedWithDiscounts.class)
                    .thenBackwardCompatible();
        }
    }

    @Nested
    final class ChangingAListIntoASet {
        private record ArticlePublished(UUID articleId, List<String> tags) {}

        private record ArticlePublishedWithTagSet(UUID articleId, Set<String> tags) {}

        @Test
        void givenAListOfDistinctElements_whenTargetReadsItAsASet_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new ArticlePublished(
                            FIXED_ID, List.of("orders", "billing", "shipping", "inventory", "returns")))
                    .whenDeserializedAs(ArticlePublishedWithTagSet.class)
                    .thenBackwardCompatible();
        }

        @Test
        void givenAListWithDuplicates_whenTargetReadsItAsASet_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new ArticlePublished(FIXED_ID, List.of("java", "java", "events")))
                            .whenDeserializedAs(ArticlePublishedWithTagSet.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Field 'tags' value differs"), message);
        }
    }

    @Nested
    final class ChangingAStringIntoAnEnum {
        private enum PaymentMethod {
            CARD,
            TRANSFER
        }

        private record PaymentRequested(UUID paymentId, String method) {}

        private record PaymentRequestedWithMethodEnum(UUID paymentId, PaymentMethod method) {}

        @Test
        void givenAStringField_whenTargetReadsItAsAnEnum_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new PaymentRequested(FIXED_ID, "CARD"))
                    .whenDeserializedAs(PaymentRequestedWithMethodEnum.class)
                    .thenBackwardCompatible(payment -> assertEquals(PaymentMethod.CARD, payment.method()));
        }
    }

    @Nested
    final class ChangingAScalarIntoAValueObject {
        private record OrderRegistered(UUID orderId, UUID customerId) {}

        private record CustomerId(UUID value) {}

        private record OrderRegisteredWithCustomerId(UUID orderId, CustomerId customerId) {}

        @Test
        void givenARawCustomerId_whenReadByTypeThatWrappedItInAValueObject_thenNotBackwardCompatible() {
            assertThrows(
                    RuntimeException.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new OrderRegistered(FIXED_ID, FIXED_ID))
                            .whenDeserializedAs(OrderRegisteredWithCustomerId.class)
                            .thenBackwardCompatible());
        }
    }

    @Nested
    final class RemovingAnEnumConstant {
        private enum InvoiceStatus {
            DRAFT,
            PAID
        }

        private record InvoiceStatusChanged(UUID invoiceId, InvoiceStatus status) {}

        @Test
        void givenAStoredEventWithARemovedStatus_whenReadByTheCurrentEnum_thenNotBackwardCompatible() {
            var path = Path.of("RemovingAnEnumConstant/InvoiceWithStatusIssuedV1.reference.snap.approved.json");

            assertThrows(
                    RuntimeException.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(MessageSnapshot.at(path))
                            .whenDeserializedAs(InvoiceStatusChanged.class)
                            .thenBackwardCompatible());
        }
    }

    @Nested
    final class RestructuringFlatDataIntoNested {
        private record ShipmentScheduled(
                UUID shipmentId, @Nullable UUID clientId) {}

        private record Recipient(UUID id) {}

        private record ShipmentScheduledNested(
                UUID shipmentId, @Nullable Recipient client) {}

        @Test
        void givenAFlatShipment_whenReadByTypeThatNestedTheClient_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new ShipmentScheduled(FIXED_ID, FIXED_ID))
                            .whenDeserializedAs(ShipmentScheduledNested.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Structural incompatibility"));
        }
    }

    @Nested
    final class EventsModelledAsAClass {
        private record StoredOrderV1(UUID orderId) {}

        static final class StoredOrder {
            public final UUID orderId;

            @JsonCreator
            StoredOrder(@JsonProperty("orderId") UUID orderId) {
                this.orderId = orderId;
            }
        }

        @Test
        void given_anEventModelledAsAClass_whenReadByANewerClassVersion_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new StoredOrderV1(FIXED_ID))
                    .whenDeserializedAs(StoredOrder.class)
                    .thenBackwardCompatible();
        }

        static final class StoredOrderWithCoupon {
            public static final String EVENT_TYPE = "OrderStored";

            public final UUID orderId;
            public final @Nullable String couponCode;

            @JsonCreator
            StoredOrderWithCoupon(
                    @JsonProperty("orderId") UUID orderId, @JsonProperty("couponCode") @Nullable String couponCode) {
                this.orderId = orderId;
                this.couponCode = couponCode;
            }
        }

        static final class StoredOrderWithCustomer {
            public final UUID orderId;
            public final String customer;

            @JsonCreator
            StoredOrderWithCustomer(@JsonProperty("orderId") UUID orderId, @JsonProperty("customer") String customer) {
                this.orderId = orderId;
                this.customer = customer;
            }
        }

        abstract static class StoredEvent {
            public final UUID eventId;

            StoredEvent(UUID eventId) {
                this.eventId = eventId;
            }
        }

        static final class StoredOrderWithEventId extends StoredEvent {
            public final UUID orderId;

            @JsonCreator
            StoredOrderWithEventId(@JsonProperty("eventId") UUID eventId, @JsonProperty("orderId") UUID orderId) {
                super(eventId);
                this.orderId = orderId;
            }
        }

        static final class StoredProduct {
            public final String code;

            StoredProduct(String code) {
                this.code = code;
            }
        }

        static final class StoredProductWithNumericCode {
            public final int code;

            @JsonCreator
            StoredProductWithNumericCode(@JsonProperty("code") int code) {
                this.code = code;
            }
        }

        @Test
        void givenARecord_whenTargetClassAddsAnOptionalField_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new StoredOrderV1(FIXED_ID))
                    .whenDeserializedAs(StoredOrderWithCoupon.class)
                    .thenBackwardCompatible(order -> assertNull(order.couponCode));
        }

        @Test
        void givenARecord_whenTargetClassAddsARequiredField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new StoredOrderV1(FIXED_ID))
                            .whenDeserializedAs(StoredOrderWithCustomer.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Required field 'customer'"), message);
        }

        @Test
        void givenARecord_whenTargetClassInheritsARequiredField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new StoredOrderV1(FIXED_ID))
                            .whenDeserializedAs(StoredOrderWithEventId.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Required field 'eventId'"), message);
        }

        @Test
        void givenAClass_whenTargetClassChangesTheTypeOfAField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new StoredProduct("007"))
                            .whenDeserializedAs(StoredProductWithNumericCode.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Field 'code' value differs"), message);
        }

        static final class StoredOrderWithCurrency {
            public final UUID orderId;
            public final String currency;

            @JsonCreator
            StoredOrderWithCurrency(@JsonProperty("orderId") UUID orderId, @JsonProperty("currency") String currency) {
                this.orderId = orderId;
                this.currency = currency;
            }
        }

        static final class StoredOrderWithOptionalCoupon {
            // A constant on the event class is not part of the message, so the check must skip it.
            public static final String NO_COUPON = "none";

            public final UUID orderId;
            public final @Nullable String couponCode;

            @JsonCreator
            StoredOrderWithOptionalCoupon(
                    @JsonProperty("orderId") UUID orderId, @JsonProperty("couponCode") @Nullable String couponCode) {
                this.orderId = orderId;
                this.couponCode = couponCode;
            }
        }

        @Test
        void givenAnEventWithoutCoupon_whenReadByAClassThatAddedOptionalCoupon_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new StoredOrderV1(FIXED_ID))
                    .whenDeserializedAs(StoredOrderWithOptionalCoupon.class)
                    .thenBackwardCompatible(order -> assertNull(order.couponCode));
        }

        @Test
        void givenAnEventWithoutCurrency_whenReadByAClassThatAddedRequiredCurrency_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(new StoredOrderV1(FIXED_ID))
                            .whenDeserializedAs(StoredOrderWithCurrency.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("currency"), message);
        }
    }

    @Nested
    final class EvolvingASealedHierarchy {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        @JsonSubTypes({@JsonSubTypes.Type(value = ShipmentCreated.class, name = "ShipmentCreated")})
        private sealed interface ShipmentEvent permits ShipmentCreated {}

        private record ShipmentCreated(UUID shipmentId) implements ShipmentEvent {}

        @Test
        void givenAStoredEventForARemovedVariant_whenReadByTheSmallerHierarchy_thenNotBackwardCompatible() {
            var path = Path.of("EvolvingASealedHierarchy/ShipmentCancelledEvent.reference.snap.approved.json");

            assertThrows(
                    RuntimeException.class,
                    () -> MessageContract.specification(Json.Jackson.defaults())
                            .given(MessageSnapshot.at(path))
                            .whenDeserializedAs(ShipmentEvent.class)
                            .thenBackwardCompatible());
        }
    }
}
