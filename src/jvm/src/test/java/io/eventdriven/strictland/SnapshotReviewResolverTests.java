package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewResolverTests {

    private final SnapshotReview perSpec = SnapshotReview.off();
    private final SnapshotReview global = SnapshotReview.approve();
    private final SnapshotReview file = SnapshotReview.tool("meld");

    private static Properties systemProps(String mode) {
        var props = new Properties();
        props.setProperty("strictland.review.mode", mode);
        return props;
    }

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    @Test
    void systemProperty_winsOverEverythingSoAnyRunCanApprove() {
        var resolved = SnapshotReviewResolver.resolve(
                perSpec, Optional.of(global), () -> Optional.of(file), systemProps("approve"));

        assertEquals(ReviewMode.APPROVE, resolved.mode());
    }

    @Test
    void perSpec_winsWhenNoSystemPropertyIsSet() {
        var resolved =
                SnapshotReviewResolver.resolve(perSpec, Optional.of(global), () -> Optional.of(file), new Properties());

        assertSame(perSpec, resolved);
    }

    @Test
    void global_winsOverFileAndDefault_whenPerSpecUnset() {
        var resolved =
                SnapshotReviewResolver.resolve(null, Optional.of(global), () -> Optional.of(file), new Properties());

        assertSame(global, resolved);
    }

    @Test
    void file_winsOverDefault_whenPerSpecAndGlobalUnset() {
        var resolved =
                SnapshotReviewResolver.resolve(null, Optional.empty(), () -> Optional.of(file), new Properties());

        assertSame(file, resolved);
    }

    @Test
    void builtInDefaultIsAuto_whenEverythingUnset() {
        var resolved = SnapshotReviewResolver.resolve(null, Optional.empty(), Optional::empty, new Properties());

        assertEquals(SnapshotReview.auto(), resolved);
    }

    @Test
    void globalDefaultReadsFromStrictland_whenSetThroughTheHolder() {
        Strictland.defaults().snapshotReview(global);

        var resolved = SnapshotReviewResolver.resolve(null);

        assertSame(global, resolved);
    }
}
