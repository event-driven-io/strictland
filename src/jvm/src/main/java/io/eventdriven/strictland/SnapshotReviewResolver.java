package io.eventdriven.strictland;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the {@link SnapshotReview} a check runs under from the configuration chain. A {@code -D}
 * system property wins so {@code -Dstrictland.review.mode=approve} re-baselines any run; otherwise the
 * order is per-spec, then the global {@link Strictland} default, then {@code strictland.properties},
 * then the built-in {@link SnapshotReview#auto()}.
 */
final class SnapshotReviewResolver {

    private SnapshotReviewResolver() {}

    static SnapshotReview resolve(@Nullable SnapshotReview perSpec) {
        return resolve(
                perSpec, Strictland.snapshotReview(), SnapshotReviewProperties::fromClasspath, System.getProperties());
    }

    static SnapshotReview resolve(
            @Nullable SnapshotReview perSpec,
            Optional<SnapshotReview> global,
            Supplier<Optional<SnapshotReview>> file,
            Properties systemProps) {
        var fromSystem = SnapshotReviewProperties.fromProperties(systemProps);
        if (fromSystem.isPresent()) {
            return fromSystem.get();
        }
        if (perSpec != null) {
            return perSpec;
        }
        return global.or(file).orElseGet(SnapshotReview::auto);
    }
}
