package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.nio.file.Path;
import java.time.OffsetDateTime;
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
            MessageContract.specification()
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
                    () -> MessageContract.specification()
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
                    () -> MessageContract.specification()
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
                    () -> MessageContract.specification()
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
            MessageContract.specification()
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
            MessageContract.specification()
                    .given(Snapshot.of(CustomerRegisteredV1.class))
                    .whenDeserializedAs(CustomerRegisteredV2.class)
                    .thenBackwardCompatible(event -> assertNull(event.referralCode()));
        }

        @Test
        void givenAStoredEventNamedByItsMessageType_whenReadByTheEvolvedType_thenBackwardCompatible() {
            MessageContract.specification()
                    .given(Snapshot.forMessageType("AccountOpenedV1", AccountOpenedV1.class))
                    .whenDeserializedAs(AccountOpenedV2.class)
                    .thenBackwardCompatible(event -> assertNull(event.currency()));
        }
    }

    @Nested
    final class ReadingAStoredOldEvent {
        private record ShoppingCartConfirmed(UUID shoppingCartId, String clientId, OffsetDateTime confirmedAt) {}

        @Test
        void givenAStoredOldEvent_whenReadByTheCurrentType_thenBackwardCompatible() {
            var path = Path.of("src/test/java/io/eventdriven/strictland/ShoppingCartConfirmedV1.approved.txt");

            MessageContract.specification()
                    .given(Snapshot.at(path, ShoppingCartConfirmed.class))
                    .whenDeserializedAs(ShoppingCartConfirmed.class)
                    .thenBackwardCompatible();
        }
    }

    @Nested
    final class WhenTheSerializerDateFormatChanged {
        private record InvoiceIssued(UUID invoiceId, OffsetDateTime confirmedAt) {}

        @Test
        void givenAnOldEpochTimestampEvent_whenReadByTheCurrentIsoType_thenNotBackwardCompatible() {
            var path = Path.of("src/test/java/io/eventdriven/strictland/InvoiceIssuedV1_EpochTimestamp.approved.txt");

            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification()
                            .given(Snapshot.at(path, InvoiceIssued.class))
                            .whenDeserializedAs(InvoiceIssued.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Field 'confirmedAt' value differs"));
        }
    }

    @Nested
    final class WideningAFieldType {
        private record PaymentCaptured(UUID paymentId, int amount) {}

        private record PaymentCapturedWiderAmount(UUID paymentId, long amount) {}

        @Test
        void givenAnIntAmount_whenReadByTypeThatWidenedItToLong_thenBackwardCompatible() {
            MessageContract.specification()
                    .given(new PaymentCaptured(FIXED_ID, 100))
                    .whenDeserializedAs(PaymentCapturedWiderAmount.class)
                    .thenBackwardCompatible();
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
                    () -> MessageContract.specification()
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
            var path = Path.of("src/test/java/io/eventdriven/strictland/InvoiceWithStatusIssuedV1.approved.txt");

            assertThrows(
                    RuntimeException.class,
                    () -> MessageContract.specification()
                            .given(Snapshot.at(path, InvoiceStatusChanged.class))
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
                    () -> MessageContract.specification()
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
            MessageContract.specification()
                    .given(new StoredOrderV1(FIXED_ID))
                    .whenDeserializedAs(StoredOrder.class)
                    .thenBackwardCompatible();
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
            var path = Path.of("src/test/java/io/eventdriven/strictland/ShipmentCancelledEvent.approved.txt");

            assertThrows(
                    RuntimeException.class,
                    () -> MessageContract.specification()
                            .given(Snapshot.at(path, ShipmentEvent.class))
                            .whenDeserializedAs(ShipmentEvent.class)
                            .thenBackwardCompatible());
        }
    }
}
