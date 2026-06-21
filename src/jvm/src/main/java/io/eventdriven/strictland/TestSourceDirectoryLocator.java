package io.eventdriven.strictland;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Finds the source directory of a test class without hard-coding a single source root, for a layout
 * that anchors a snapshot to the test's own source folder. A compiled class carries no source path - the JVM keeps
 * only the source file's name, not its directory - so the directory is resolved from the known source
 * roots a build uses by convention.
 *
 * <p>The default checks each configured root for the package directory that holds the test's source
 * file, covering the standard {@code src/test/java}, {@code src/test/kotlin}, and {@code src/test/scala}
 * roots. A project with a non-standard layout passes its own roots to {@link #knownRoots(java.util.List)}.
 * Resolution is a direct existence check against those roots, so it never scans the build tree and never
 * depends on the working directory's contents.</p>
 */
@FunctionalInterface
interface TestSourceDirectoryLocator {

    /** The source roots a JVM build uses by convention, checked in order. */
    List<String> DEFAULT_SOURCE_ROOTS = List.of("src/test/java", "src/test/kotlin", "src/test/scala");

    /**
     * Returns the directory holding the test class's source file, with its package path included.
     *
     * @param packageName the package of the test asking for the snapshot
     * @param sourceFileName the test's source file name (for example {@code OrderTests.kt}), or {@code
     *     null} when the JVM did not record it
     * @return the test's source directory
     * @throws IllegalStateException when no configured root holds the test's source
     */
    Path locate(String packageName, @Nullable String sourceFileName);

    /**
     * The default locator, checking the {@linkplain #DEFAULT_SOURCE_ROOTS conventional source roots}.
     *
     * @return a locator over the default source roots
     */
    static TestSourceDirectoryLocator knownRoots() {
        return knownRoots(DEFAULT_SOURCE_ROOTS);
    }

    /**
     * A locator that checks the given source roots in order and returns the package directory of the
     * first one that holds the test's source file. When the JVM did not record the source file name, it
     * falls back to the first root whose package directory exists.
     *
     * @param sourceRoots the source roots to check, in order
     * @return a locator over those roots
     */
    static TestSourceDirectoryLocator knownRoots(List<String> sourceRoots) {
        return (packageName, sourceFileName) -> {
            var packagePath = packageName.replace('.', '/');
            for (var root : sourceRoots) {
                var packageDir = Path.of(root, packagePath);
                var holdsTheSource = sourceFileName != null
                        ? Files.isRegularFile(packageDir.resolve(sourceFileName))
                        : Files.isDirectory(packageDir);
                if (holdsTheSource) {
                    return packageDir.normalize();
                }
            }
            throw new IllegalStateException("Could not locate the test source directory for package " + packageName
                    + (sourceFileName != null ? " holding " + sourceFileName : "") + " under any of " + sourceRoots
                    + ".");
        };
    }
}
