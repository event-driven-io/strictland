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

    static Path resolve(String snapshotName) {
        var callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.filter(f -> !DSL_CLASSES.contains(f.getClassName()))
                        .filter(f -> !f.getClassName().startsWith("java."))
                        .filter(f -> !f.getClassName().startsWith("jdk."))
                        .filter(f -> !f.getClassName().startsWith("sun."))
                        .filter(f -> !f.getClassName().startsWith("org.junit."))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Cannot determine calling test class from stack")))
                .getDeclaringClass();

        var packagePath = callerClass.getPackageName().replace('.', '/');
        return Path.of(SOURCE_ROOT, packagePath, snapshotName + ".approved.txt");
    }
}
