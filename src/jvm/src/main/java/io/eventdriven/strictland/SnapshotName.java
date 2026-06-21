package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * The settled snapshot file name, as parts. It renders the convention base name
 * {@code {shortMessageTypeName}.{version}.{testClass}.{testName}[.{variant}]} and the stable read prefix
 * {@code {shortMessageTypeName}.{version}.} that a replay-all read globs. It is the one place the grammar
 * lives, so naming and reading never drift apart.
 *
 * @param shortMessageTypeName the short, dotless contract name the snapshot locks down
 * @param version the version label the snapshot is pinned to
 * @param testClass the test class that owns the snapshot, simple or fully qualified per the layout
 * @param testName the test method that owns the snapshot
 * @param variant the label distinguishing several snapshots of one contract in one test, or {@code null}
 */
record SnapshotName(
        String shortMessageTypeName,
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
        var tail = variant != null ? testClass + "." + testName + "." + variant : testClass + "." + testName;
        return readPrefix(shortMessageTypeName, version) + tail;
    }

    /**
     * The stable name prefix every snapshot of one contract version shares, which a replay-all read
     * globs.
     *
     * @param shortMessageTypeName the short, dotless contract name the snapshot locks down
     * @param version the version label the snapshot is pinned to
     * @return {@code shortMessageTypeName.version.}
     */
    static String readPrefix(String shortMessageTypeName, String version) {
        return shortMessageTypeName + "." + version + ".";
    }

    /**
     * The short, dotless tail of a message type, the part after the last dot. A dotless type is its own
     * short name.
     *
     * @param messageType the message type's fully-qualified name
     * @return the part after the last dot, or the whole type when it has none
     */
    static String shortName(String messageType) {
        var dot = messageType.lastIndexOf('.');
        return dot < 0 ? messageType : messageType.substring(dot + 1);
    }

    /**
     * The namespace of a message type, the part before the last dot. A dotless type has no namespace.
     *
     * @param messageType the message type's fully-qualified name
     * @return the part before the last dot, or an empty string when it has none
     */
    static String namespace(String messageType) {
        var dot = messageType.lastIndexOf('.');
        return dot < 0 ? "" : messageType.substring(0, dot);
    }
}
