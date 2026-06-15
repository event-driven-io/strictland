package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class GlobalRootLayoutEndToEndTests {

    private static final UUID SHIPMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private record ShipmentScheduled(UUID shipmentId, String carrier) {}

    @BeforeEach
    void setGlobalDefault() {
        Strictland.defaults()
                .snapshotLayout(SnapshotLayout.globalRoot("src/test/resources/snapshots")
                        .grouping(Grouping.PER_CONTRACT));
    }

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    @Test
    void pinsTheMessage_thenReadsItBackThroughACompatibilityCheck() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new ShipmentScheduled(SHIPMENT_ID, "Acme"))
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(Snapshot.of(ShipmentScheduled.class))
                .whenDeserializedAs(ShipmentScheduled.class)
                .thenBackwardCompatible(shipment -> assertEquals("Acme", shipment.carrier()));
    }
}
