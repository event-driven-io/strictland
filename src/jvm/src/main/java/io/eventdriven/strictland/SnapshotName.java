package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * The canonical name of a snapshot, derived only from the contract it locks down: the message type, the
 * version, and an optional variant. It renders the convention base name and the stable read prefix, and
 * resolves the file's path against a {@link SnapshotLayout}, so naming and reading never drift apart.
 *
 * <p>One snapshot exists per {@code (messageType, version, variant)}. The base name is {@code
 * {shortName}.{version}} when there's no variant, or {@code {shortName}.{version}.{variant}} when there
 * is. A read globs the stable {@code {shortName}.{version}.} prefix and replays every match.</p>
 *
 * @param messageType the message type the snapshot locks down, split into namespace and short name
 * @param version the version label the snapshot is pinned to
 * @param variant the variant naming this snapshot among others of the same type and version, or {@code
 *     null} when there's just one
 */
record SnapshotName(
        MessageTypeName messageType,
        String version,
        @Nullable String variant) {

    private static final String SNAP_APPROVED_SUFFIX = ".snap.approved";

    /**
     * Builds a canonical name from a message type's name, splitting it into namespace and short name.
     *
     * @param messageType the message type's name the snapshot locks down
     * @param version the version label the snapshot is pinned to
     * @param variant the variant naming this snapshot, or {@code null} when there's just one
     * @return the canonical name for that contract
     */
    static SnapshotName of(String messageType, String version, @Nullable String variant) {
        return new SnapshotName(MessageTypeName.of(messageType), version, variant);
    }

    /**
     * Renders the convention base name: {@code {shortName}.{version}}, with the variant appended when
     * present.
     *
     * @return the convention base name, without folder, {@code .snap.approved}, or extension
     */
    String base() {
        var prefix = messageType.shortName() + "." + version;
        return variant == null || variant.isBlank() ? prefix : prefix + "." + variant;
    }

    /**
     * The stable name prefix every snapshot of one contract version shares, which a replay-all read
     * globs.
     *
     * @return {@code {shortName}.{version}.}
     */
    String readPrefix() {
        return messageType.shortName() + "." + version + ".";
    }

    /**
     * Resolves the committed approved-file path against a layout: the folder the layout lays out, then
     * the base name with {@code .snap.approved} and the serializer's extension.
     *
     * @param layout the layout that lays out the snapshot tree
     * @param fileExtension the extension the serializer writes, including the leading dot
     * @return the committed approved-file path
     */
    Path resolve(SnapshotLayout layout, String fileExtension) {
        return folder(layout).resolve(base() + SNAP_APPROVED_SUFFIX + fileExtension);
    }

    /**
     * The directory a read globs in, the folder the layout lays out for this message type.
     *
     * @param layout the layout that lays out the snapshot tree
     * @return the directory the matching approved files live in
     */
    Path folder(SnapshotLayout layout) {
        return layout.folder(messageType);
    }
}
