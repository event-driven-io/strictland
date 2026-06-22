package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotConstantsTests {

    @Test
    void unsetVariant_isTheReservedFamilyReadSentinel() {
        assertEquals("unset", Snapshot.UNSET_VARIANT);
    }

    @Test
    void defaultVariant_isTheReservedOnDiskVariant() {
        assertEquals("default", Snapshot.DEFAULT_VARIANT);
    }
}
