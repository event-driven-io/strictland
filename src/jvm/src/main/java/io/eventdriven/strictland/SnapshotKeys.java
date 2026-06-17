package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * Maps a contract's message type and optional variant label to the key a {@link SnapshotStorage} reads
 * and writes under, so the DSL decides a snapshot's identity and storage stays a thin sink. The
 * default file storage receives a resolved file path; a custom storage receives a logical name it maps
 * to its own location.
 */
@FunctionalInterface
interface SnapshotKeys {

    /**
     * Resolves the storage key for a contract, folding a variant label into it so several snapshots of
     * one type sit side by side without overwriting each other.
     *
     * @param messageTypeName the logical name of the message being locked down
     * @param variantLabel a label distinguishing one snapshot from another, or {@code null} for the
     *     default snapshot
     * @return the key handed to {@link SnapshotStorage}
     */
    String resolve(String messageTypeName, @Nullable String variantLabel);
}
