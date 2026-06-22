package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotVariantTests {

    @Test
    void unset_isTheFamilyReadMarker() {
        assertInstanceOf(SnapshotVariant.Unset.class, SnapshotVariant.UNSET);
    }

    @Test
    void defaultVariant_isAFirstClassVariantNotALabel() {
        assertInstanceOf(SnapshotVariant.Default.class, SnapshotVariant.DEFAULT);
    }

    @Test
    void named_wrapsTheLabelInAByLabel() {
        assertEquals(new SnapshotVariant.ByLabel("withCoupon"), SnapshotVariant.named("withCoupon"));
    }
}
