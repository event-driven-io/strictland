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
     * Approves and persists a snapshot at one exact location. The first run saves it for you to review
     * and commit; on a later run a matching payload passes, and one that has drifted throws an {@link
     * AssertionError} to fail the check.
     *
     * @param location the exact file the snapshot is stored at
     * @param data the serialized message to approve and persist
     * @throws AssertionError when the payload differs from the approved snapshot
     */
    void store(SnapshotLocation location, SnapshotData data) throws AssertionError;

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
