package io.eventdriven.strictland;

import java.util.Optional;

interface SnapshotStorage {
    void store(String name, byte[] payload);

    Optional<byte[]> read(String name);
}
