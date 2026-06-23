package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class MessageSnapshotTests {

    private record OrderPlaced(String orderId) {}

    @Test
    void of_aClass_buildsAnUnpinnedByClassReference() {
        var snapshot = MessageSnapshot.of(OrderPlaced.class);

        assertEquals(MessageSnapshot.DEFAULT_VERSION, snapshot.version());
        assertEquals(SnapshotVariant.UNSET, snapshot.variant());
    }

    @Test
    void of_aMessageInstance_buildsTheInHandValueArm() {
        var message = new OrderPlaced("o-1");

        var snapshot = MessageSnapshot.of(message);

        MessageSnapshot.OfInstance<?> inHand = assertInstanceOf(MessageSnapshot.OfInstance.class, snapshot);
        assertEquals(message, inHand.message());
    }

    @Test
    void byClassVariantByLabel_wrapsTheLabelInAByLabel() {
        var snapshot = MessageSnapshot.of(OrderPlaced.class).variant("withCoupon");

        assertEquals(SnapshotVariant.named("withCoupon"), snapshot.variant());
    }

    @Test
    void byClassVariantOverload_setsTheVariantDirectly() {
        var snapshot = MessageSnapshot.of(OrderPlaced.class).variant(SnapshotVariant.DEFAULT);

        assertEquals(SnapshotVariant.DEFAULT, snapshot.variant());
    }

    @Test
    void byMessageTypeVariantOverload_setsTheVariantDirectly() {
        var snapshot = MessageSnapshot.ofTypeNamed("OrderPlaced").variant(SnapshotVariant.named("withCoupon"));

        assertEquals(SnapshotVariant.named("withCoupon"), snapshot.variant());
    }

    @Test
    void version_pinsTheVersionAndKeepsTheVariant() {
        var snapshot =
                MessageSnapshot.of(OrderPlaced.class).variant("withCoupon").version("2");

        assertEquals("2", snapshot.version());
        assertEquals(SnapshotVariant.named("withCoupon"), snapshot.variant());
    }
}
