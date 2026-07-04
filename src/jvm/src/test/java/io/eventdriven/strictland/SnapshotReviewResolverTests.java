package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewResolverTests {

    private final SnapshotReview runtime = SnapshotReview.approve();
    private final SnapshotReview perSpec = SnapshotReview.off();
    private final SnapshotReview global = SnapshotReview.tool("meld");
    private final SnapshotReview file = SnapshotReview.tool("vscode");

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    @Test
    void runtimeOverride_winsOverEverythingSoAnyRunCanApprove() {
        var resolved =
                SnapshotReviewResolver.resolve(Optional.of(runtime), perSpec, Optional.of(global), Optional.of(file));

        assertSame(runtime, resolved);
    }

    @Test
    void perSpec_winsWhenThereIsNoRuntimeOverride() {
        var resolved =
                SnapshotReviewResolver.resolve(Optional.empty(), perSpec, Optional.of(global), Optional.of(file));

        assertSame(perSpec, resolved);
    }

    @Test
    void global_winsOverFileWhenPerSpecUnset() {
        var resolved = SnapshotReviewResolver.resolve(Optional.empty(), null, Optional.of(global), Optional.of(file));

        assertSame(global, resolved);
    }

    @Test
    void file_winsWhenPerSpecAndGlobalUnset() {
        var resolved = SnapshotReviewResolver.resolve(Optional.empty(), null, Optional.empty(), Optional.of(file));

        assertSame(file, resolved);
    }

    @Test
    void defaultsToAuto_whenEverythingUnset() {
        var resolved = SnapshotReviewResolver.resolve(Optional.empty(), null, Optional.empty(), Optional.empty());

        assertEquals(SnapshotReview.auto(), resolved);
    }

    @Test
    void productionResolve_readsTheGlobalDefaultFromStrictland() {
        Strictland.defaults().snapshotReview(global);

        var resolved = SnapshotReviewResolver.resolve(null);

        assertSame(global, resolved);
    }
}
