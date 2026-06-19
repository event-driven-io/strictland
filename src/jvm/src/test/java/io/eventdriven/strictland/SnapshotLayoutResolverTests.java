package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLayoutResolverTests {

    private final SnapshotLayout perSpec = SnapshotLayout.nextToTest();
    private final SnapshotLayout global = SnapshotLayout.globalRoot("global");
    private final SnapshotLayout file = SnapshotLayout.globalRoot("file");

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    @Test
    void perSpec_winsOverEverything() {
        var resolved = SnapshotLayoutResolver.resolve(perSpec, Optional.of(global), () -> Optional.of(file));

        assertSame(perSpec, resolved);
    }

    @Test
    void global_winsOverFileAndDefault_whenPerSpecUnset() {
        var resolved = SnapshotLayoutResolver.resolve(null, Optional.of(global), () -> Optional.of(file));

        assertSame(global, resolved);
    }

    @Test
    void file_winsOverDefault_whenPerSpecAndGlobalUnset() {
        var resolved = SnapshotLayoutResolver.resolve(null, Optional.empty(), () -> Optional.of(file));

        assertSame(file, resolved);
    }

    @Test
    void builtInDefaultIsNextToTest_whenEverythingUnset() {
        var resolved = SnapshotLayoutResolver.resolve(null, Optional.empty(), Optional::empty);

        assertEquals(SnapshotLayout.nextToTest(), resolved);
    }

    @Test
    void globalDefaultReadsFromStrictland_whenSetThroughTheHolder() {
        Strictland.defaults().snapshotLayout(global);

        var resolved = SnapshotLayoutResolver.resolve(null);

        assertSame(global, resolved);
    }

    @Test
    void resetDefaults_restoresBuiltInDefault() {
        Strictland.defaults().snapshotLayout(global);
        Strictland.resetDefaults();

        var resolved = SnapshotLayoutResolver.resolve(null);

        assertEquals(SnapshotLayout.nextToTest(), resolved);
    }

    @Test
    void fileLevelReadsFromClasspathFixture_throughTheInjectableSeam() {
        var resolved = SnapshotLayoutResolver.resolve(
                null,
                Optional.empty(),
                () -> SnapshotLayoutProperties.fromClasspath("fixtures/layout-sample.properties"));

        assertEquals(SnapshotRoot.GLOBAL_ROOT, resolved.location());
        assertEquals(SnapshotGrouping.PER_CONTRACT, resolved.grouping());
        assertEquals("src/test/resources/snapshots", resolved.rootPath());
        assertEquals(TestClassNaming.SIMPLE, resolved.testClassNaming());
    }

    @Test
    void perSpec_layoutWithFullTestClassNaming_isThreadedThrough() {
        var qualified = SnapshotLayout.nextToTest().testClassNaming(TestClassNaming.FULL);

        var resolved = SnapshotLayoutResolver.resolve(qualified, Optional.of(global), () -> Optional.of(file));

        assertSame(qualified, resolved);
        assertEquals(TestClassNaming.FULL, resolved.testClassNaming());
    }
}
