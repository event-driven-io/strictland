package io.eventdriven.strictland;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

final class SnapshotReviewResolver {

    private SnapshotReviewResolver() {}

    static SnapshotReview resolve(@Nullable SnapshotReview perSpec) {
        return resolve(
                perSpec,
                Strictland.snapshotReview(),
                SnapshotReviewProperties::fromClasspath,
                System.getProperties(),
                System.getenv());
    }

    static SnapshotReview resolve(
            @Nullable SnapshotReview perSpec,
            Optional<SnapshotReview> global,
            Supplier<Optional<SnapshotReview>> file,
            Properties systemProps) {
        return resolve(perSpec, global, file, systemProps, Map.of());
    }

    static SnapshotReview resolve(
            @Nullable SnapshotReview perSpec,
            Optional<SnapshotReview> global,
            Supplier<Optional<SnapshotReview>> file,
            Properties systemProps,
            Map<String, String> env) {
        var fromRuntime = SnapshotReviewProperties.fromPropertiesAndEnv(systemProps, env);
        if (fromRuntime.isPresent()) {
            return fromRuntime.get();
        }
        if (perSpec != null) {
            return perSpec;
        }
        return global.or(file).orElseGet(SnapshotReview::auto);
    }
}
