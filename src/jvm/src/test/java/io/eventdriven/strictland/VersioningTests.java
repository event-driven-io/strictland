package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.orders.OrderPlaced;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class VersioningTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);

    @Test
    void givenAVersionPinnedOnTheInstance_whenSerialized_theSnapshotCarriesThatVersion() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new OrderPlaced(FIXED_ID, "Alice", FIXED_DATE), "2")
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(MessageSnapshot.of(OrderPlaced.class).version("2"))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }

    @Test
    void givenAVersionPinnedOnAMessageTypeSnapshot_whenSerialized_theSnapshotCarriesThatVersion() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new OrderPlaced(FIXED_ID, "Alice", FIXED_DATE))
                .whenSerializedAs(MessageSnapshot.ofTypeNamed("OrderShipped").version("3"))
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(MessageSnapshot.ofTypeNamed("OrderShipped").version("3"))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }

    @Test
    void givenNoApprovedSnapshotForTheVersion_whenRead_thenFailsBecauseNothingMatches() {
        var error = assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.ofTypeNamed("NeverWritten").version("9"))
                        .whenDeserializedAs(OrderPlaced.class)
                        .thenBackwardCompatible());

        assertEquals("Cannot read snapshot file: NeverWritten.9.", error.getMessage());
    }
}
