package io.eventdriven.strictland;

/**
 * The bytes of a single snapshot. A custom {@link SnapshotStorage} is handed one to persist in {@code
 * store} and returns one from {@code read} and {@code readAll}.
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
