package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class RegistryLayoutEndToEndTests {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private record OrderPlaced(UUID orderId, String customer) {}

    private static SpecificationOptions options() {
        return Json.Jackson.defaults().snapshotLayout(SnapshotLayout.registry());
    }

    @Test
    void pinsTheMessage_thenReadsItBackThroughACompatibilityCheck() {
        MessageContract.specification(options())
                .given(new OrderPlaced(ORDER_ID, "Alice"))
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(options())
                .given(MessageSnapshot.of(OrderPlaced.class))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }
}
