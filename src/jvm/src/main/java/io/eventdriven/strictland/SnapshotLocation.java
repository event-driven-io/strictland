package io.eventdriven.strictland;

import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Resolves a snapshot's committed file path from a {@link SnapshotLayout}, the test that asks for it,
 * and the message it locks down. It owns the parts that storage should not: finding the calling test
 * class on the stack, finding that test's source directory, and applying the layout. Storage is then
 * handed a fully resolved path and writes bytes at it.
 */
final class SnapshotLocation implements SnapshotKeys {

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
        this(layout, fileExtension, TestSourceDirectoryLocator.approvalTests());
    }

    SnapshotLocation(SnapshotLayout layout, String fileExtension, TestSourceDirectoryLocator sourceDirectoryLocator) {
        this.layout = layout;
        this.fileExtension = fileExtension;
        this.sourceDirectoryLocator = sourceDirectoryLocator;
    }

    @Override
    public String resolve(String messageTypeName, @Nullable String variantLabel) {
        var caller = caller();
        var testSourceDir =
                layout.strategy() == SnapshotLayout.Strategy.GLOBAL_ROOT ? null : sourceDirectoryLocator.locate(caller);
        return layout.resolve(
                        testSourceDir,
                        caller.getPackageName(),
                        caller.getSimpleName(),
                        messageTypeName,
                        variantLabel,
                        fileExtension)
                .toString();
    }

    private static Class<?> caller() {
        var frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.filter(f -> !DSL_CLASSES.contains(f.getClassName()))
                        .findFirst());
        return requireCaller(frame);
    }

    static Class<?> requireCaller(Optional<StackWalker.StackFrame> frame) {
        return frame.orElseThrow(() -> new IllegalStateException("Cannot determine calling test class from stack"))
                .getDeclaringClass();
    }
}
