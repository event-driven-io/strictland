package io.eventdriven.strictland;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
final class ForwardCompatibilityTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Nested
    final class AddingAFieldOldConsumersIgnore {
        private record OrderPlaced(UUID orderId, String customer) {}

        private record OrderPlacedWithCoupon(UUID orderId, String customer, String couponCode) {}

        @Test
        void givenNewOrderWithCoupon_whenReadByOldTypeWithoutCoupon_thenForwardCompatible() {
            Contract.specification()
                    .given(new OrderPlacedWithCoupon(FIXED_ID, "Alice", "SAVE10"))
                    .whenDeserializedAs(OrderPlaced.class)
                    .thenForwardCompatible(order -> assertEquals("Alice", order.customer()));
        }
    }

    @Nested
    final class MakingARequiredFieldOptional {
        private record OrderConfirmedRequiredCurrency(UUID orderId, String currency) {}

        private record OrderConfirmedOptionalCurrency(
                UUID orderId, @Nullable String currency) {}

        @Test
        void givenNewOrderOmittingTheNowOptionalCurrency_whenReadByOldRequiredType_thenNotForwardCompatible() {
            // NON_NULL drops the null currency from the JSON entirely, so the old required reader
            // genuinely sees the key as absent rather than present-and-null.
            var omitNulls =
                    JsonMapper.builder().serializationInclusion(NON_NULL).build();

            var error = assertThrows(
                    AssertionError.class,
                    () -> Contract.specification(omitNulls)
                            .given(new OrderConfirmedOptionalCurrency(FIXED_ID, null))
                            .whenDeserializedAs(OrderConfirmedRequiredCurrency.class)
                            .thenForwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Required field") && message.contains("currency"));
        }
    }

    @Nested
    final class AddingAnEnumConstant {
        private enum OrderStatusV2 {
            PLACED,
            CONFIRMED,
            SHIPPED
        }

        private enum OrderStatusV1 {
            PLACED,
            CONFIRMED
        }

        private record OrderStatusChangedV2(UUID orderId, OrderStatusV2 status) {}

        private record OrderStatusChangedV1(UUID orderId, OrderStatusV1 status) {}

        @Test
        void givenNewOrderWithAShippedStatus_whenReadByOldEnumWithoutIt_thenNotForwardCompatible() {
            assertThrows(
                    RuntimeException.class,
                    () -> Contract.specification()
                            .given(new OrderStatusChangedV2(FIXED_ID, OrderStatusV2.SHIPPED))
                            .whenDeserializedAs(OrderStatusChangedV1.class)
                            .thenForwardCompatible());
        }
    }

    @Nested
    final class AStrictConsumerRejectsUnknownFields {
        private record OrderReceivedWithCoupon(UUID orderId, String customer, String couponCode) {}

        private record OrderReceived(UUID orderId, String customer) {}

        @Test
        void givenNewOrderWithExtraField_whenReadByOldTypeUsingAStrictMapper_thenNotForwardCompatible() {
            var strictMapper = JsonMapper.builder()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            assertThrows(
                    RuntimeException.class,
                    () -> Contract.specification(strictMapper)
                            .given(new OrderReceivedWithCoupon(FIXED_ID, "Alice", "SAVE10"))
                            .whenDeserializedAs(OrderReceived.class)
                            .thenForwardCompatible());
        }

        @Test
        void givenNewOrderWithExtraField_whenReadByOldTypeUsingTheLenientDefaultMapper_thenForwardCompatible() {
            Contract.specification()
                    .given(new OrderReceivedWithCoupon(FIXED_ID, "Alice", "SAVE10"))
                    .whenDeserializedAs(OrderReceived.class)
                    .thenForwardCompatible();
        }
    }

    @Nested
    final class RestructuringNestedDataBackToFlat {
        private record Recipient(UUID id) {}

        private record ShipmentScheduledNested(
                UUID shipmentId, @Nullable Recipient client) {}

        private record ShipmentScheduled(
                UUID shipmentId, @Nullable UUID clientId) {}

        @Test
        void givenANestedShipment_whenReadByOldFlatType_thenNotForwardCompatible() {
            var error = assertThrows(
                    AssertionError.class,
                    () -> Contract.specification()
                            .given(new ShipmentScheduledNested(FIXED_ID, new Recipient(FIXED_ID)))
                            .whenDeserializedAs(ShipmentScheduled.class)
                            .thenForwardCompatible());

            var message = requireNonNull(error.getMessage());
            assertTrue(message.contains("Structural incompatibility"));
        }
    }

    @Nested
    final class AddingASealedVariant {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        @JsonSubTypes({@JsonSubTypes.Type(value = ShipmentCreated.class, name = "ShipmentCreated")})
        private sealed interface ShipmentEvent permits ShipmentCreated {}

        private record ShipmentCreated(UUID shipmentId) implements ShipmentEvent {}

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        @JsonSubTypes({
            @JsonSubTypes.Type(value = ShipmentCreatedV2.class, name = "ShipmentCreated"),
            @JsonSubTypes.Type(value = ShipmentDelivered.class, name = "ShipmentDelivered")
        })
        private sealed interface ShipmentEventV2 permits ShipmentCreatedV2, ShipmentDelivered {}

        private record ShipmentCreatedV2(UUID shipmentId) implements ShipmentEventV2 {}

        private record ShipmentDelivered(UUID shipmentId) implements ShipmentEventV2 {}

        @Test
        void givenANewVariant_whenReadByOldHierarchyThatDoesNotKnowIt_thenNotForwardCompatible() {
            assertThrows(
                    RuntimeException.class,
                    () -> Contract.specification()
                            .given(new ShipmentDelivered(FIXED_ID))
                            .whenDeserializedAs(ShipmentEvent.class)
                            .thenForwardCompatible());
        }
    }

    @Nested
    final class ChangingACollectionElementType {
        private record OrderTaggedWithStrings(UUID orderId, List<String> tags) {}

        private record OrderTaggedWithNumbers(UUID orderId, List<Integer> tags) {}

        @Test
        void givenNewStringTags_whenReadByOldTypeExpectingNumbers_thenNotForwardCompatible() {
            assertThrows(
                    RuntimeException.class,
                    () -> Contract.specification()
                            .given(new OrderTaggedWithStrings(FIXED_ID, List.of("urgent")))
                            .whenDeserializedAs(OrderTaggedWithNumbers.class)
                            .thenForwardCompatible());
        }
    }
}
