package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
final class CsvMessageSerializerTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private interface Csv {
        static SpecificationOptions defaults() {
            return SpecificationOptions.serializer(new CsvMessageSerializer());
        }
    }

    private record OrderPlacedCsv(UUID orderId, String customer) {}

    private record OrderPlacedCsvWithCoupon(
            UUID orderId, String customer, @Nullable String couponCode) {}

    private record OrderPlacedCsvValidated(UUID orderId, String customer) {
        private OrderPlacedCsvValidated {
            if (customer.isBlank()) {
                throw new IllegalArgumentException("customer must not be blank");
            }
        }
    }

    @Test
    void given_csvEvent_whenSerialized_shapeIsPinned() {
        MessageContract.specification(Csv.defaults())
                .given(new OrderPlacedCsv(FIXED_ID, "Alice"))
                .whenSerialized(Snapshot.forMessageType("OrderPlacedCsv"))
                .thenContractIsUnchanged();
    }

    @Test
    void given_csvOrderWithoutCoupon_whenReadByTypeThatAddedOptionalCoupon_thenBackwardCompatible() {
        MessageContract.specification(Csv.defaults())
                .given(new OrderPlacedCsv(FIXED_ID, "Alice"))
                .whenDeserializedAs(OrderPlacedCsvWithCoupon.class)
                .thenBackwardCompatible(order -> assertNull(order.couponCode()));
    }

    @Test
    void given_csvTokenThatIsNotAUuid_whenDeserializedIntoUuidComponent_thenThrows() {
        var serializer = new CsvMessageSerializer();
        var bytes = "not-a-uuid,Alice".getBytes(UTF_8);

        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(bytes, OrderPlacedCsv.class));
    }

    @Test
    void given_csvTokensThatTheCanonicalConstructorRejects_whenDeserialized_thenWrappedThrows() {
        var serializer = new CsvMessageSerializer();
        var roundTrips = serializer.serialize(new OrderPlacedCsvValidated(FIXED_ID, "Alice"));
        assertEquals(FIXED_ID + ",Alice", new String(roundTrips, UTF_8));

        var blankCustomerBytes = (FIXED_ID + ",").getBytes(UTF_8);
        var error = assertThrows(
                RuntimeException.class,
                () -> serializer.deserialize(blankCustomerBytes, OrderPlacedCsvValidated.class));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("CSV deserialization as OrderPlacedCsvValidated failed"));
    }
}
