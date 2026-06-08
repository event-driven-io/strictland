package io.eventdriven.strictland;

import java.nio.file.Path;
import java.util.Set;

class SnapshotPathResolver {
    private static final Set<String> DSL_CLASSES = Set.of(
            Contract.class.getName(),
            GivenStep.class.getName(),
            ThenContractStep.class.getName(),
            ThenCompatibilityStep.class.getName(),
            SnapshotPathResolver.class.getName());
    private static final String SOURCE_ROOT = "src/test/java";

    private SnapshotPathResolver() {}

    static Path resolve(String snapshotName) {
        var frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.filter(f -> !DSL_CLASSES.contains(f.getClassName()))
                        .findFirst());
        var callerClass = requireCaller(frame);
        var packagePath = callerClass.getPackageName().replace('.', '/');
        return Path.of(SOURCE_ROOT, packagePath, snapshotName + ".approved.txt");
    }

    static Class<?> requireCaller(java.util.Optional<StackWalker.StackFrame> frame) {
        return frame.orElseThrow(() -> new IllegalStateException("Cannot determine calling test class from stack"))
                .getDeclaringClass();
    }
}
