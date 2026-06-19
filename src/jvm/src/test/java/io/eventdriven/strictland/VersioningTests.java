package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class VersioningTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private record OrderPlaced(UUID orderId, String customer) {}

    @Test
    void givenAVersionPinnedOnTheInstance_whenSerialized_theSnapshotCarriesThatVersion() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new OrderPlaced(FIXED_ID, "Alice"), "2")
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(Snapshot.of(OrderPlaced.class).version("2"))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }

    @Test
    void givenAVersionPinnedOnAMessageTypeSnapshot_whenSerialized_theSnapshotCarriesThatVersion() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new OrderPlaced(FIXED_ID, "Alice"))
                .whenSerializedAs(Snapshot.forMessageType("OrderShipped").version("3"))
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(Snapshot.forMessageType("OrderShipped").version("3"))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }

    @Test
    void givenNoApprovedSnapshotForTheVersion_whenRead_thenFailsBecauseNothingMatches() {
        var error = assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(Snapshot.forMessageType("NeverWritten").version("9"))
                        .whenDeserializedAs(OrderPlaced.class)
                        .thenBackwardCompatible());

        assertEquals("Cannot read snapshot file: NeverWritten.9.", error.getMessage());
    }
}
