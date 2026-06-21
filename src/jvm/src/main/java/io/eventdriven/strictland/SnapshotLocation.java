package io.eventdriven.strictland;

import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Composes a snapshot's committed file name and resolves its path, from the calling test on the stack
 * and the message it locks down. It owns the parts that storage should not: finding the calling test
 * class, naming the file by the settled convention, and resolving it against the {@link
 * SnapshotLayout} registry. Storage is then handed a fully resolved path, or a bare base name when a
 * custom store decides its own paths.
 *
 * <p>An owned snapshot's name is {@code {shortMessageTypeName}.{version}.{testClass}.{testName}}, with
 * the variant appended when present. A read globs the stable {@code {shortMessageTypeName}.{version}.}
 * prefix and replays every match, so a snapshot one test wrote can be read by another. When the layout
 * is {@code null}, a custom store is in play: name composition still runs, but the path resolution is
 * skipped and the bare base name is returned.</p>
 */
final class SnapshotLocation {

    private static final Set<String> DSL_CLASSES = Set.of(
            MessageContract.class.getName(),
            GivenStep.class.getName(),
            ThenContractStep.class.getName(),
            ThenCompatibilityStep.class.getName(),
            SnapshotLocation.class.getName());

    private final @Nullable SnapshotLayout layout;
    private final String fileExtension;
    private final TestClassNaming testClassNaming;

    SnapshotLocation(SnapshotLayout layout, String fileExtension) {
        this(layout, fileExtension, layout.testClassNaming());
    }

    SnapshotLocation(@Nullable SnapshotLayout layout, String fileExtension, TestClassNaming testClassNaming) {
        this.layout = layout;
        this.fileExtension = fileExtension;
        this.testClassNaming = testClassNaming;
    }

    /**
     * Composes the owned snapshot's name and resolves its write path, walking the stack once for the
     * calling test. Returns the full resolved path when a layout is in play, or the bare convention base
     * name when a custom store decides its own paths.
     */
    String resolveForWrite(String messageType, String version, @Nullable String variant) {
        var caller = requireCaller(caller());
        var baseName = baseName(caller, messageType, version, variant);
        if (layout == null) {
            return baseName;
        }
        return layout.resolve(messageType, baseName, fileExtension).toString();
    }

    /**
     * Resolves the folder and stable name prefix a read globs to replay every matching snapshot. The
     * folder is {@code null} for custom storage, leaving the prefix to carry the convention base name.
     * When {@code variant} is set, the read keeps only files whose base name ends with that label.
     *
     * @param messageType the contract name the snapshot locks down
     * @param version the version label the snapshot is pinned to
     * @param variant the label selecting just that labelled variant, or {@code null} to replay all
     * @return where a read finds the approved snapshots to replay
     */
    SnapshotReadLocation resolveForRead(String messageType, String version, @Nullable String variant) {
        var prefix = SnapshotName.readPrefix(SnapshotName.shortName(messageType), version);
        if (layout == null) {
            return new SnapshotReadLocation(null, prefix, variant);
        }
        var anyName = layout.resolve(messageType, prefix, fileExtension);
        return new SnapshotReadLocation(anyName.getParent(), prefix, variant);
    }

    private String baseName(
            StackWalker.StackFrame caller, String messageType, String version, @Nullable String variant) {
        var callerClass = caller.getDeclaringClass();
        var testClass = testClassNaming == TestClassNaming.FULL ? callerClass.getName() : callerClass.getSimpleName();
        var testName = caller.getMethodName();
        return new SnapshotName(SnapshotName.shortName(messageType), version, testClass, testName, variant).base();
    }

    private static Optional<StackWalker.StackFrame> caller() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.filter(f -> !DSL_CLASSES.contains(f.getClassName()))
                        .findFirst());
    }

    static StackWalker.StackFrame requireCaller(Optional<StackWalker.StackFrame> frame) {
        return frame.orElseThrow(() -> new IllegalStateException("Cannot determine calling test class from stack"));
    }
}
