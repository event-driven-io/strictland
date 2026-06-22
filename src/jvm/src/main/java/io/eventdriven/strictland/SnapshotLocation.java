package io.eventdriven.strictland;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The exact identity of one snapshot file, always relative to the storage root. It carries the relative
 * folder segments, the three-part file stem {@code {shortName}.{version}.{variant}}, and the
 * serializer's extension, so a store and a read of the same snapshot always agree on the same file.
 *
 * <p>The storage owns the root, the {@code .snap.approved} marker, and the {@code .received} sibling.
 * This value owns only what the contract decides: where the file sits and what it is called. A registry
 * snapshot maps its message type's namespace to folders ending in the short name; an explicit {@code
 * at(path)} snapshot decomposes the given root-relative path instead.</p>
 *
 * @param path the relative folder segments under the storage root: the namespace parts, then the short
 *     name
 * @param name the file stem, always {@code {shortName}.{version}.{variant}}, without the marker or
 *     extension
 * @param extension the extension the serializer writes, including the leading dot
 */
public record SnapshotLocation(List<String> path, String name, String extension) {

    private static final String SNAP_APPROVED_MARKER = ".snap.approved.";
    private static final String DEFAULT_LABEL = "default";

    /**
     * Builds the location for a registry snapshot: the namespace parts then the short name as folder
     * segments, and the three-part {@code {shortName}.{version}.{variant}} stem. An {@link
     * SnapshotVariant.Unset} variant, like {@link SnapshotVariant#DEFAULT}, renders the reserved {@code
     * default} label, since an unnamed write lands under the default variant rather than a family.
     *
     * @param messageType the message type the snapshot locks down, split into namespace and short name
     * @param version the version label the snapshot is pinned to
     * @param variant the variant naming this snapshot, rendered to its on-disk label
     * @param extension the extension the serializer writes, including the leading dot
     * @return the exact location for that contract
     */
    static SnapshotLocation of(MessageTypeName messageType, String version, SnapshotVariant variant, String extension) {
        var folder = new ArrayList<String>();
        var namespace = messageType.namespace();
        if (!namespace.isEmpty()) {
            var start = 0;
            var dot = namespace.indexOf('.');
            while (dot >= 0) {
                folder.add(namespace.substring(start, dot));
                start = dot + 1;
                dot = namespace.indexOf('.', start);
            }
            folder.add(namespace.substring(start));
        }
        folder.add(messageType.shortName());
        var resolvedVariant = label(variant);
        var name = messageType.shortName() + "." + version + "." + resolvedVariant;
        return new SnapshotLocation(List.copyOf(folder), name, extension);
    }

    private static String label(SnapshotVariant variant) {
        return switch (variant) {
            case SnapshotVariant.Unset ignored -> DEFAULT_LABEL;
            case SnapshotVariant.Default ignored -> DEFAULT_LABEL;
            case SnapshotVariant.ByLabel byLabel -> byLabel.name();
        };
    }

    /**
     * Builds the location for an explicit {@code at(path)} snapshot from a root-relative path. The path's
     * parent becomes the folder segments, and its file name splits on the {@code .snap.approved.} marker
     * into the stem and the extension. A file name without that marker falls back to splitting on its
     * last dot for the extension.
     *
     * <p>Re-joining {@code root / path / name + ".snap.approved" + extension} reproduces {@code
     * root.resolve(relativePath)}, so naming and reading never drift apart.</p>
     *
     * @param relativePath the snapshot file's path, relative to the storage root
     * @return the exact location the path points at
     */
    static SnapshotLocation ofRelativePath(Path relativePath) {
        var folder = new ArrayList<String>();
        var parent = relativePath.getParent();
        if (parent != null) {
            for (var segment : parent) {
                folder.add(segment.toString());
            }
        }
        var fileName = relativePath.getFileName().toString();
        var markerAt = fileName.indexOf(SNAP_APPROVED_MARKER);
        String name;
        String extension;
        if (markerAt >= 0) {
            name = fileName.substring(0, markerAt);
            extension = fileName.substring(markerAt + SNAP_APPROVED_MARKER.length() - 1);
        } else {
            var dot = fileName.lastIndexOf('.');
            name = dot < 0 ? fileName : fileName.substring(0, dot);
            extension = dot < 0 ? "" : fileName.substring(dot);
        }
        return new SnapshotLocation(List.copyOf(folder), name, extension);
    }
}
