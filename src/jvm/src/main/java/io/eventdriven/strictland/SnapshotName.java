package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * The settled snapshot file name, as parts. It renders the convention base name
 * {@code {messageType}.{version}.{testClass}.{testName}[.{variant}]} and the stable read prefix
 * {@code {messageType}.{version}.} that a replay-all read globs. It is the one place the grammar lives,
 * so naming and reading never drift apart.
 *
 * @param messageType the contract name the snapshot locks down
 * @param version the version label the snapshot is pinned to
 * @param testClass the test class that owns the snapshot, simple or fully qualified per the layout
 * @param testName the test method that owns the snapshot
 * @param variant the label distinguishing several snapshots of one contract in one test, or {@code null}
 */
record SnapshotName(
        String messageType,
        String version,
        String testClass,
        String testName,
        @Nullable String variant) {

    /**
     * Renders the convention base name: the read prefix, then {@code testClass.testName}, then the
     * variant when one is set.
     *
     * @return the convention base name, without folder, {@code .snap.approved}, or extension
     */
    String base() {
        var discriminator = variant != null ? testClass + "." + testName + "." + variant : testClass + "." + testName;
        return readPrefix(messageType, version) + discriminator;
    }

    /**
     * The stable name prefix every snapshot of one contract version shares, which a replay-all read
     * globs.
     *
     * @param messageType the contract name the snapshot locks down
     * @param version the version label the snapshot is pinned to
     * @return {@code messageType.version.}
     */
    static String readPrefix(String messageType, String version) {
        return messageType + "." + version + ".";
    }
}
