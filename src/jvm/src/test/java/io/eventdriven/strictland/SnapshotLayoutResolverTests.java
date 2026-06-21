package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLayoutResolverTests {

    private final SnapshotLayout perSpec = SnapshotLayout.registry();
    private final SnapshotLayout global = SnapshotLayout.registry().rootPath("global");
    private final SnapshotLayout file = SnapshotLayout.registry().rootPath("file");

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
    void builtInDefaultIsRegistry_whenEverythingUnset() {
        var resolved = SnapshotLayoutResolver.resolve(null, Optional.empty(), Optional::empty);

        assertEquals(SnapshotLayout.registry(), resolved);
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

        assertEquals(SnapshotLayout.registry(), resolved);
    }

    @Test
    void fileLevelReadsFromClasspathFixture_throughTheInjectableSeam() {
        var resolved = SnapshotLayoutResolver.resolve(
                null,
                Optional.empty(),
                () -> SnapshotLayoutProperties.fromClasspath("fixtures/layout-sample.properties"));

        assertEquals("src/test/resources", resolved.rootPath());
        assertEquals("approved", resolved.wrapperFolder());
    }

    @Test
    void perSpec_layoutWithWrapperFolderOverride_isThreadedThrough() {
        var overridden = SnapshotLayout.registry().wrapperFolder("approved");

        var resolved = SnapshotLayoutResolver.resolve(overridden, Optional.of(global), () -> Optional.of(file));

        assertSame(overridden, resolved);
        assertEquals("approved", resolved.wrapperFolder());
    }
}
