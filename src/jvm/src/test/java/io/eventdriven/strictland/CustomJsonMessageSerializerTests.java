package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
final class CustomJsonMessageSerializerTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final SpecificationOptions OPTIONS =
            SpecificationOptions.serializer(new CustomJsonMessageSerializer());

    private record ShoppingCartConfirmedV1(
            UUID shoppingCartId, @Nullable String clientId, OffsetDateTime confirmedAt) {}

    private record ShoppingCartConfirmedV3(
            UUID shoppingCartId, @Nullable String clientId, OffsetDateTime confirmedAt, String confirmedBy) {}

    @Test
    void givenAMessage_whenANewerVersionAddsARequiredField_thenNotBackwardCompatible() {
        var error = assertThrows(
                AssertionError.class,
                () -> MessageContract.specification(OPTIONS)
                        .given(new ShoppingCartConfirmedV1(FIXED_ID, "anonymised", FIXED_DATE))
                        .whenDeserializedAs(ShoppingCartConfirmedV3.class)
                        .thenBackwardCompatible());

        assertTrue(requireNonNull(error.getMessage()).contains("confirmedBy"), error.getMessage());
    }

    @Nested
    final class BackwardCompatibility {
        private record OrderPlaced(UUID orderId, String customer) {}

        private record OrderPlacedWithCoupon(
                UUID orderId, String customer, @Nullable String couponCode) {}

        private record OrderPlacedWithCurrency(UUID orderId, String customer, String currency) {}

        private record OrderPlacedWithQuantity(UUID orderId, String customer, int quantity) {}

        private record OrderPlacedRenamed(UUID orderId, String client) {}

        private record InvoiceIssued(UUID invoiceId, OffsetDateTime confirmedAt) {}

        @Test
        void givenAMessage_whenTargetAddsAnOptionalField_thenBackwardCompatible() {
            MessageContract.specification(OPTIONS)
                    .given(new OrderPlaced(FIXED_ID, "Alice"))
                    .whenDeserializedAs(OrderPlacedWithCoupon.class)
                    .thenBackwardCompatible(order -> assertNull(order.couponCode()));
        }

        @Test
        void givenAMessage_whenTargetAddsARequiredField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(OPTIONS)
                            .given(new OrderPlaced(FIXED_ID, "Alice"))
                            .whenDeserializedAs(OrderPlacedWithCurrency.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Required field") && message.contains("currency"), message);
        }

        @Test
        void givenAMessage_whenTargetAddsARequiredPrimitiveField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(OPTIONS)
                            .given(new OrderPlaced(FIXED_ID, "Alice"))
                            .whenDeserializedAs(OrderPlacedWithQuantity.class)
                            .thenBackwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Required field") && message.contains("quantity"), message);
        }

        @Test
        void givenAMessage_whenTargetRenamesAField_thenNotBackwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(OPTIONS)
                            .given(new OrderPlaced(FIXED_ID, "Alice"))
                            .whenDeserializedAs(OrderPlacedRenamed.class)
                            .thenBackwardCompatible());

            assertTrue(requireNonNull(error.getMessage()).contains("client"), error.getMessage());
        }

        @Test
        void givenASnapshotReferencedByPath_whenTargetReadsADateWrittenInAnotherFormat_thenNotBackwardCompatible() {
            assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(OPTIONS)
                            .given(
                                    MessageSnapshot.at(
                                            Path.of(
                                                    "WhenTheSerializerDateFormatChanged/InvoiceIssuedV1_EpochTimestamp.reference.snap.approved.json")))
                            .whenDeserializedAs(InvoiceIssued.class)
                            .thenBackwardCompatible(invoice -> assertEquals(FIXED_DATE, invoice.confirmedAt())));
        }
    }

    @Nested
    final class ForwardCompatibility {
        private record OrderShipped(UUID orderId, String customer) {}

        private record OrderShippedWithCoupon(UUID orderId, String customer, String couponCode) {}

        private record Recipient(UUID id) {}

        private record ShipmentScheduledNested(
                UUID shipmentId, @Nullable Recipient client) {}

        private record ShipmentScheduled(
                UUID shipmentId, @Nullable UUID clientId) {}

        @Test
        void givenAMessageWithAnExtraField_whenReadByAnOlderTypeWithoutIt_thenForwardCompatible() {
            MessageContract.specification(OPTIONS)
                    .given(new OrderShippedWithCoupon(FIXED_ID, "Alice", "SAVE10"))
                    .whenDeserializedAs(OrderShipped.class)
                    .thenForwardCompatible(order -> assertEquals("Alice", order.customer()));
        }

        @Test
        void givenANestedMessage_whenReadByAnOlderFlatType_thenNotForwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> MessageContract.specification(OPTIONS)
                            .given(new ShipmentScheduledNested(FIXED_ID, new Recipient(FIXED_ID)))
                            .whenDeserializedAs(ShipmentScheduled.class)
                            .thenForwardCompatible());

            assertTrue(requireNonNull(error.getMessage()).contains("Structural incompatibility"), error.getMessage());
        }
    }
}
