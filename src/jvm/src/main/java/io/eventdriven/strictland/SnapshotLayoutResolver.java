package io.eventdriven.strictland;

import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

final class SnapshotLayoutResolver {

    private SnapshotLayoutResolver() {}

    static SnapshotLayout resolve(@Nullable SnapshotLayout perSpec) {
        return resolve(perSpec, Strictland.snapshotLayout(), SnapshotLayoutProperties::fromClasspath);
    }

    static SnapshotLayout resolve(
            @Nullable SnapshotLayout perSpec,
            Optional<SnapshotLayout> global,
            Supplier<Optional<SnapshotLayout>> file) {
        if (perSpec != null) {
            return perSpec;
        }
        return global.or(file).orElseGet(SnapshotLayout::flat);
    }
}
