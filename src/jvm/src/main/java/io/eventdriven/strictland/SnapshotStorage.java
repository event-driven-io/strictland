package io.eventdriven.strictland;

import java.util.List;

/**
 * Stores an approved snapshot and reads it back, keyed by typed values. Normally you don't implement it:
 * the default file-based storage keeps each snapshot in the committed file in git repository. Implement it
 * only to keep snapshots somewhere else, such as a shared fixture directory or an in-memory store for a
 * binary format. The contract checks read and write through it.
 *
 * <p>The DSL hands in typed locations and filters: a {@link SnapshotLocation} names one exact file, a
 * {@link SnapshotFilter} names a whole family of variants. A custom store derives its own key from the
 * location's {@link SnapshotLocation#name() name} and matches a filter by its {@link
 * SnapshotFilter#namePrefix() name prefix}. It needs no concept of a root, layout,
 * or marker.</p>
 *
 * <p>Pass your storage to {@link SpecificationOptions#snapshotStorage(SnapshotStorage)}, or take the
 * default from {@link Snapshots#files()}.</p>
 *
 * <pre>
 * MessageContract.specification(
 *         SpecificationOptions.serializer(new JacksonMessageSerializer(mapper))
 *             .snapshotStorage(Snapshots.files()))
 *     .given(new OrderPlaced(orderId, "Alice", placedAt))
 *     .whenSerialized()
 *     .thenContractIsUnchanged();
 * </pre>
 */
public interface SnapshotStorage {

    /**
     * Stores a snapshot at one exact location and reports what happened, without deciding what a drift
     * means. The first run writes the approved snapshot ({@link SnapshotResult.Approved}); a later run
     * that matches passes ({@link SnapshotResult.Unchanged}); one that differs writes the received
     * payload beside the approved one and reports the drift ({@link SnapshotResult.Drifted}) for the
     * caller to open, fail, or accept. It never throws to fail a check - only wraps a genuine I/O
     * failure.
     *
     * @param location the exact file the snapshot is stored at
     * @param data the serialized message to store
     * @return what storing did: approved, unchanged, or drifted
     */
    SnapshotResult store(SnapshotLocation location, SnapshotData data);

    /**
     * Re-baselines a location by overwriting its approved snapshot with a payload you've accepted, and
     * clearing any received file. This is how {@code approve} review mode accepts an intended change; it
     * assumes the caller has already decided the drift is wanted.
     *
     * @param location the exact file to re-baseline
     * @param data the accepted payload to store as the new approved snapshot
     */
    void approve(SnapshotLocation location, SnapshotData data);

    /**
     * Reads back the one snapshot approved at an exact location, for a compatibility check that replays a
     * pinned version's data. Throws when the expected approved file is missing, since a pinned read names
     * a single file that must exist.
     *
     * @param location the exact file to read
     * @return the stored snapshot's payload
     * @throws RuntimeException when the expected approved file cannot be read
     */
    SnapshotData read(SnapshotLocation location);

    /**
     * Reads every approved snapshot in a family, the variants sharing one message type and version, in a
     * deterministic order. A compatibility check replays them all, so a snapshot one test wrote can be
     * read by another.
     *
     * @param filter the family to read: the shared folder path and the variants' name prefix
     * @return the matching payloads in a deterministic order, empty when nothing matches
     */
    List<SnapshotData> readAll(SnapshotFilter filter);
}
