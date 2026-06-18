package io.eventdriven.strictland;

import java.util.Optional;
import java.util.Set;

/**
 * Resolves a snapshot's committed file path from a {@link SnapshotLayout}, the test that asks for it,
 * and the snapshot's identity. It owns the parts that storage should not: finding the calling test
 * class on the stack, finding that test's source directory, and applying the layout. Storage is then
 * handed a fully resolved path and writes bytes at it.
 *
 * <p>It takes a snapshot's identity as a {@code (messageType, snapshotName)} pair the DSL has already
 * settled: {@code messageType} groups it, {@code snapshotName} names its file. Higher-level notions such
 * as a variant are folded into the name before they reach here, so this type stays unaware of them.
 */
final class SnapshotLocation {

    private static final Set<String> DSL_CLASSES = Set.of(
            MessageContract.class.getName(),
            GivenStep.class.getName(),
            ThenContractStep.class.getName(),
            ThenCompatibilityStep.class.getName(),
            SnapshotLocation.class.getName());

    private final SnapshotLayout layout;
    private final String fileExtension;
    private final TestSourceDirectoryLocator sourceDirectoryLocator;

    SnapshotLocation(SnapshotLayout layout, String fileExtension) {
        this(layout, fileExtension, TestSourceDirectoryLocator.knownRoots());
    }

    SnapshotLocation(SnapshotLayout layout, String fileExtension, TestSourceDirectoryLocator sourceDirectoryLocator) {
        this.layout = layout;
        this.fileExtension = fileExtension;
        this.sourceDirectoryLocator = sourceDirectoryLocator;
    }

    String resolve(String messageType, String snapshotName) {
        var caller = requireCaller(caller());
        var callerClass = caller.getDeclaringClass();
        var testSourceDir = layout.strategy() == SnapshotLayout.Strategy.GLOBAL_ROOT
                ? null
                : sourceDirectoryLocator.locate(callerClass.getPackageName(), caller.getFileName());
        return layout.resolve(
                        testSourceDir,
                        callerClass.getPackageName(),
                        callerClass.getSimpleName(),
                        messageType,
                        snapshotName,
                        fileExtension)
                .toString();
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
