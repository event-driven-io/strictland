package io.eventdriven.strictland;

import java.util.Optional;

/**
 * Where approved snapshots live, keyed by a logical name. Supply your own to keep them somewhere
 * other than next to your test - an inline string, a shared fixture directory - and the contract
 * checks read and write through it.
 *
 * <p>Hand an implementation to {@link SpecificationOptions#snapshotStorage(SnapshotStorage)}; for the
 * default of a file beside your test, reach for {@link Snapshots#files()}.
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
     * Approves and persists a payload under a logical name: the first run saves it for you to review
     * and commit, a later run that matches passes, and one that has drifted fails the check.
     *
     * @param name the logical name the snapshot is stored under
     * @param payload the serialized message to approve and persist
     */
    void store(String name, byte[] payload);

    /**
     * Reads back the payload approved under a logical name, for a compatibility check that replays an
     * older version's data.
     *
     * @param name the logical name to read
     * @return the stored payload, or {@link Optional#empty()} when nothing has been approved yet
     */
    Optional<byte[]> read(String name);
}
