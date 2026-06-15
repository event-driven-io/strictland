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
        var layout = SnapshotLayout.nextToTest();

        Strictland.defaults().snapshotLayout(layout);

        assertSame(layout, Strictland.snapshotLayout().orElseThrow());
    }

    @Test
    void snapshotLayout_whenReset_returnsToUnset() {
        Strictland.defaults().snapshotLayout(SnapshotLayout.nextToTest());

        Strictland.resetDefaults();

        assertTrue(Strictland.snapshotLayout().isEmpty());
    }

    @Test
    void config_chainsAndReturnsSameInstance() {
        var config = Strictland.defaults();

        var returned = config.snapshotLayout(SnapshotLayout.nextToTest());

        assertSame(config, returned);
        assertEquals(
                SnapshotLayout.Strategy.NEXT_TO_TEST,
                Strictland.snapshotLayout().orElseThrow().strategy());
    }
}
