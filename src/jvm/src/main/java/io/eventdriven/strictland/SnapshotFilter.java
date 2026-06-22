package io.eventdriven.strictland;

import java.util.ArrayList;
import java.util.List;

/**
 * The read query for a whole family of snapshots: every variant pinned to one {@code (messageType,
 * version)}. It carries the relative folder {@code path} the variants share, the namespace parts then
 * the short name, composed the same way {@link SnapshotLocation} lays out its folder, and a {@code
 * namePrefix} every variant's file name starts with.
 *
 * <p>This is a family read, a prefix match, not an exact single-variant read. Reading one named variant
 * is a different path, an exact location read against a resolved file. Keeping the family query to a
 * folder path plus a name prefix keeps glob and regex out of the public API.</p>
 *
 * @param path the relative folder the family's variants share: the namespace parts, then the short name
 * @param namePrefix the prefix every variant's file name starts with: {@code {shortName}.{version}.}
 */
public record SnapshotFilter(List<String> path, String namePrefix) {

    /**
     * Builds the family read for one message type and version: the shared folder path and the {@code
     * {shortName}.{version}.} name prefix every variant carries. A dotless message type has an empty
     * namespace, so its path is just the short name.
     *
     * @param messageType the message type the family locks down, split into namespace and short name
     * @param version the version label the family is pinned to
     * @return the family read for that message type and version
     */
    static SnapshotFilter of(MessageTypeName messageType, String version) {
        var path = new ArrayList<String>();
        var namespace = messageType.namespace();
        if (!namespace.isEmpty()) {
            var start = 0;
            var dot = namespace.indexOf('.');
            while (dot >= 0) {
                path.add(namespace.substring(start, dot));
                start = dot + 1;
                dot = namespace.indexOf('.', start);
            }
            path.add(namespace.substring(start));
        }
        path.add(messageType.shortName());
        var namePrefix = messageType.shortName() + "." + version + ".";
        return new SnapshotFilter(List.copyOf(path), namePrefix);
    }
}
