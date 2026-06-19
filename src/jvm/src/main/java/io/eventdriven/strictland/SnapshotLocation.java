package io.eventdriven.strictland;

import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Composes a snapshot's committed file name and resolves its path, from the calling test on the stack
 * and the message it locks down. It owns the parts that storage should not: finding the calling test
 * class, finding that test's source directory, naming the file by the settled convention, and applying
 * the {@link SnapshotLayout}. Storage is then handed a fully resolved path, or a bare base name when a
 * custom store decides its own paths.
 *
 * <p>An owned snapshot's name is {@code {messageType}.{version}.{discriminator}}, where the
 * discriminator is {@code {testClass}.{testName}} with the variant appended when present. A read globs
 * the stable {@code {messageType}.{version}.} prefix and replays every match, so a snapshot one test
 * wrote can be read by another. When the layout is {@code null}, a custom store is in play: name
 * composition still runs, but the path resolution is skipped and the bare base name is returned.</p>
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
    private final TestSourceDirectoryLocator sourceDirectoryLocator;

    SnapshotLocation(SnapshotLayout layout, String fileExtension) {
        this(layout, fileExtension, TestSourceDirectoryLocator.knownRoots());
    }

    SnapshotLocation(SnapshotLayout layout, String fileExtension, TestSourceDirectoryLocator sourceDirectoryLocator) {
        this(layout, fileExtension, layout.testClassNaming(), sourceDirectoryLocator);
    }

    SnapshotLocation(
            @Nullable SnapshotLayout layout,
            String fileExtension,
            TestClassNaming testClassNaming,
            TestSourceDirectoryLocator sourceDirectoryLocator) {
        this.layout = layout;
        this.fileExtension = fileExtension;
        this.testClassNaming = testClassNaming;
        this.sourceDirectoryLocator = sourceDirectoryLocator;
    }

    String resolve(String messageType, String snapshotName) {
        var caller = requireCaller(caller());
        var callerClass = caller.getDeclaringClass();
        var resolvedLayout = requireLayout();
        var testSourceDir = resolvedLayout.location() == SnapshotRoot.GLOBAL_ROOT
                ? null
                : sourceDirectoryLocator.locate(callerClass.getPackageName(), caller.getFileName());
        return resolvedLayout
                .resolve(
                        testSourceDir,
                        callerClass.getPackageName(),
                        callerClass.getSimpleName(),
                        messageType,
                        snapshotName,
                        fileExtension)
                .toString();
    }

    /**
     * Composes the owned snapshot's name and resolves its write path, walking the stack once for the
     * calling test. Returns the full resolved path when a layout is in play, or the bare convention base
     * name when a custom store decides its own paths.
     */
    String resolveForWrite(String messageType, String version, @Nullable String variant) {
        var caller = requireCaller(caller());
        var callerClass = caller.getDeclaringClass();
        var baseName = baseName(caller, messageType, version, variant);
        if (layout == null) {
            return baseName;
        }
        var testSourceDir = layout.location() == SnapshotRoot.GLOBAL_ROOT
                ? null
                : sourceDirectoryLocator.locate(callerClass.getPackageName(), caller.getFileName());
        return layout.resolve(
                        testSourceDir,
                        callerClass.getPackageName(),
                        callerClass.getSimpleName(),
                        messageType,
                        baseName,
                        fileExtension)
                .toString();
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
        var prefix = SnapshotName.readPrefix(messageType, version);
        if (layout == null) {
            return new SnapshotReadLocation(null, prefix, variant);
        }
        var caller = requireCaller(caller());
        var callerClass = caller.getDeclaringClass();
        var testSourceDir = layout.location() == SnapshotRoot.GLOBAL_ROOT
                ? null
                : sourceDirectoryLocator.locate(callerClass.getPackageName(), caller.getFileName());
        var anyName = layout.resolve(
                testSourceDir,
                callerClass.getPackageName(),
                callerClass.getSimpleName(),
                messageType,
                prefix,
                fileExtension);
        var folder = anyName.getParent();
        return new SnapshotReadLocation(folder, prefix, variant);
    }

    private String baseName(
            StackWalker.StackFrame caller, String messageType, String version, @Nullable String variant) {
        var callerClass = caller.getDeclaringClass();
        var testClass = testClassNaming == TestClassNaming.FULL ? callerClass.getName() : callerClass.getSimpleName();
        var testName = caller.getMethodName();
        return new SnapshotName(messageType, version, testClass, testName, variant).base();
    }

    private SnapshotLayout requireLayout() {
        if (layout == null) {
            throw new IllegalStateException("No layout configured for this snapshot location");
        }
        return layout;
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
