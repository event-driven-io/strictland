package io.eventdriven.strictland;

import java.util.List;
import java.util.Optional;

/**
 * Stores an approved snapshot and reads it back, keyed by name. Normally you don't implement it: the
 * default file-based storage keeps each snapshot in the committed contract registry. Implement it only to keep
 * snapshots somewhere else, such as a shared fixture directory or an in-memory store for a binary
 * format. The contract checks read and write through it.
 *
 * <p>The key the DSL hands in is already resolved: the layout, the calling test, and any variant label
 * are folded in before storage is called, so this extension point stays two methods and a custom
 * storage needs no concept of layouts or variants.</p>
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
     * Approves and persists a payload under a name. The first run saves it for you to review and
     * commit; on a later run a matching payload passes, and one that has drifted throws an {@link
     * AssertionError} to fail the check.
     *
     * @param name the name the snapshot is stored under
     * @param payload the serialized message to approve and persist
     * @throws AssertionError when the payload differs from the approved snapshot
     */
    void store(String name, byte[] payload) throws AssertionError;

    /**
     * Reads back the payload approved under a name, for a compatibility check that replays an older
     * version's data.
     *
     * @param name the logical name to read
     * @return the stored payload, or {@link Optional#empty()} when nothing has been approved yet
     */
    Optional<byte[]> read(String name);

    /**
     * Reads every approved snapshot at a read-location, the matches sharing a stable name prefix, in a
     * deterministic order. A compatibility check replays them all, so a snapshot one test wrote can be
     * read by another.
     *
     * <p>The default reads by the location's {@code prefix} as an exact name, enough for a custom store
     * that keys snapshots by that name. File-backed storage overrides it to glob the location's folder.</p>
     *
     * @param location the folder and name prefix the matching snapshots share
     * @return the matching payloads in a deterministic order, empty when nothing matches
     */
    default List<byte[]> readAll(SnapshotReadLocation location) {
        return read(location.prefix()).map(List::of).orElseGet(List::of);
    }
}
