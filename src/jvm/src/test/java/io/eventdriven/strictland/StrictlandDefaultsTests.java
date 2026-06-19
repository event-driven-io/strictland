package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
    void testSourceRoots_whenUnset_areTheConventionalRoots() {
        assertEquals(TestSourceDirectoryLocator.DEFAULT_SOURCE_ROOTS, Strictland.testSourceRoots());
    }

    @Test
    void testSourceRoots_whenSet_areReadBack() {
        Strictland.defaults().testSourceRoots(List.of("modules/orders/src/test/java"));

        assertEquals(List.of("modules/orders/src/test/java"), Strictland.testSourceRoots());
    }

    @Test
    void testSourceRoots_whenReset_returnToTheConventionalRoots() {
        Strictland.defaults().testSourceRoots(List.of("custom/root"));

        Strictland.resetDefaults();

        assertEquals(TestSourceDirectoryLocator.DEFAULT_SOURCE_ROOTS, Strictland.testSourceRoots());
    }

    @Test
    void config_chainsAndReturnsSameInstance() {
        var config = Strictland.defaults();

        var returned = config.snapshotLayout(SnapshotLayout.nextToTest());

        assertSame(config, returned);
        assertEquals(
                SnapshotRoot.NEXT_TO_TEST,
                Strictland.snapshotLayout().orElseThrow().location());
    }
}
