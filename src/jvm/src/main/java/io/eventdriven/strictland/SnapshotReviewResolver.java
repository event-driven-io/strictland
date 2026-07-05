package io.eventdriven.strictland;

import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

final class SnapshotReviewResolver {

    private SnapshotReviewResolver() {}

    static SnapshotReview resolve(@Nullable SnapshotReview perSpec) {
        return resolve(
                SnapshotReviewProperties.fromProperties(System.getProperties()),
                perSpec,
                Strictland.snapshotReview(),
                SnapshotReviewProperties.fromClasspath());
    }

    /**
     * Picks the first review setting in precedence order - a runtime override (system property or
     * environment variable), then this spec's own setting, then the programmatic global default, then a
     * classpath file - falling back to {@link SnapshotReview#auto()} when nothing is configured.
     */
    static SnapshotReview resolve(
            Optional<SnapshotReview> runtime,
            @Nullable SnapshotReview perSpec,
            Optional<SnapshotReview> global,
            Optional<SnapshotReview> file) {
        return Stream.of(runtime, Optional.ofNullable(perSpec), global, file)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(SnapshotReview::auto);
    }
}
