package io.eventdriven.strictland;

/**
 * A snapshot's raw payload bytes, the one value storage stores, reads, and replays. It is a thin typed
 * wrapper so the storage seam moves a named value rather than a bare {@code byte[]}, which keeps method
 * signatures self-describing and stops a payload getting confused with any other byte array.
 */
public final class SnapshotData {

    private final byte[] bytes;

    /**
     * Wraps a snapshot's raw payload bytes.
     *
     * @param bytes the snapshot's raw payload bytes
     */
    public SnapshotData(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns the snapshot's raw payload bytes, the value storage writes and reads back.
     *
     * @return the snapshot's raw payload bytes
     */
    public byte[] bytes() {
        return bytes;
    }
}
