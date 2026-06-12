package io.eventdriven.strictland;

import java.util.Optional;

/**
 * Stores an approved snapshot and reads it back, keyed by name. Normally you don't implement it: the
 * default file-based storage keeps each snapshot next to your test. Implement it only to keep
 * snapshots somewhere else, such as a shared fixture directory or an in-memory store for a binary
 * format. The contract checks read and write through it.
 *
 * <p>Pass your storage to {@link SpecificationOptions#snapshotStorage(SnapshotStorage)}, or take the
 * default from {@link Snapshots#files()}.
 *
 * {@snippet :
 * MessageContract.specification(
 *         SpecificationOptions.serializer(new JacksonMessageSerializer(mapper))
 *             .snapshotStorage(Snapshots.files()))
 *     .given(new OrderPlaced(orderId, "Alice", placedAt))
 *     .whenSerialized()
 *     .thenContractIsUnchanged();
 * }
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
}
