package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotConstantsTests {

    private static final MessageTypeName TYPE = MessageTypeName.of("OrderPlaced");

    @Test
    void unsetVariant_isTheFamilyReadMarker() {
        assertInstanceOf(SnapshotVariant.Unset.class, SnapshotVariant.UNSET);
    }

    @Test
    void defaultVariant_rendersTheReservedOnDiskLabel() {
        var location = SnapshotLocation.of(TYPE, "1", SnapshotVariant.DEFAULT, ".json");

        assertEquals("OrderPlaced.1.default", location.name());
    }

    @Test
    void unsetVariant_alsoRendersTheReservedOnDiskLabel() {
        var location = SnapshotLocation.of(TYPE, "1", SnapshotVariant.UNSET, ".json");

        assertEquals("OrderPlaced.1.default", location.name());
    }
}
