package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class MessageSnapshotTests {

    @Test
    void byMessageTypeVariantOverload_setsTheVariantDirectly() {
        var snapshot = MessageSnapshot.forMessageType("OrderPlaced").variant(SnapshotVariant.named("withCoupon"));

        assertEquals(SnapshotVariant.named("withCoupon"), snapshot.variant());
    }
}
