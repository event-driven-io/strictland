package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.eventdriven.strictland.examples.shipping.ShipmentScheduled;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class RegistryLayoutEndToEndTests {

    private static final UUID SHIPMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);

    private static SpecificationOptions options() {
        return Json.Jackson.defaults().snapshotLayout(SnapshotLayout.registry());
    }

    @Test
    void pinsTheMessage_thenReadsItBackThroughACompatibilityCheck() {
        MessageContract.specification(options())
                .given(new ShipmentScheduled(SHIPMENT_ID, "Alice Smith", FIXED_DATE))
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(options())
                .given(MessageSnapshot.of(ShipmentScheduled.class))
                .whenDeserializedAs(ShipmentScheduled.class)
                .thenBackwardCompatible(shipment -> assertEquals("Alice Smith", shipment.recipientName()));
    }
}
