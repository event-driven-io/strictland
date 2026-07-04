package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class StrictlandDefaultsTests {

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    @Test
    void snapshotLayout_whenUnset_isEmpty() {
        assertTrue(Strictland.snapshotLayout().isEmpty());
    }

    @Test
    void snapshotLayout_whenSet_isReadBack() {
        var layout = SnapshotLayout.registry();

        Strictland.defaults().snapshotLayout(layout);

        assertSame(layout, Strictland.snapshotLayout().orElseThrow());
    }

    @Test
    void snapshotLayout_whenReset_returnsToUnset() {
        Strictland.defaults().snapshotLayout(SnapshotLayout.registry());

        Strictland.resetDefaults();

        assertTrue(Strictland.snapshotLayout().isEmpty());
    }

    @Test
    void config_chainsAndReturnsSameInstance() {
        var config = Strictland.defaults();

        var returned = config.snapshotLayout(SnapshotLayout.registry().wrapperFolder("approved"));

        assertSame(config, returned);
        assertEquals("approved", Strictland.snapshotLayout().orElseThrow().wrapperFolder());
    }

    @Test
    void snapshotReview_whenUnset_isEmpty() {
        assertTrue(Strictland.snapshotReview().isEmpty());
    }

    @Test
    void snapshotReview_whenSet_isReadBack() {
        var review = SnapshotReview.approve();

        Strictland.defaults().snapshotReview(review);

        assertSame(review, Strictland.snapshotReview().orElseThrow());
    }

    @Test
    void snapshotReview_whenReset_returnsToUnset() {
        Strictland.defaults().snapshotReview(SnapshotReview.approve());

        Strictland.resetDefaults();

        assertTrue(Strictland.snapshotReview().isEmpty());
    }

    @Test
    void config_snapshotReview_chainsAndReturnsSameInstance() {
        var config = Strictland.defaults();

        var returned = config.snapshotReview(SnapshotReview.off());

        assertSame(config, returned);
        assertEquals(ReviewMode.OFF, Strictland.snapshotReview().orElseThrow().mode());
    }
}
