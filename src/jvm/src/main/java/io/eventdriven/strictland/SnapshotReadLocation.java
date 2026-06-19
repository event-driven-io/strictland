package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Where a read finds the approved snapshots to replay, as a folder plus the stable name prefix every
 * matching file shares. A read globs the folder for files whose name starts with {@code prefix} and
 * replays each, so a snapshot one test wrote can be read by another.
 *
 * <p>The {@code folder} is {@code null} for custom storage that decides its own paths: then {@code
 * prefix} carries the convention base name on its own, and a custom store reads by exact name.</p>
 *
 * @param folder the directory the matching approved files live in, or {@code null} for custom storage
 * @param prefix the stable name prefix the matching files share, {@code messageType.version.}
 * @param variant the label selecting just that labelled variant, or {@code null} to replay all
 */
record SnapshotReadLocation(
        @Nullable Path folder, String prefix, @Nullable String variant) {
    SnapshotReadLocation(@Nullable Path folder, String prefix) {
        this(folder, prefix, null);
    }
}
