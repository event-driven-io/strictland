package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Finds the source directory of a test class, so a snapshot can be anchored beside the test that owns
 * it without hard-coding {@code src/test/java}. A compiled class carries no source path, so the answer
 * is a guess from build conventions; this seam isolates that guess behind one method.
 *
 * <p>The default reuses ApprovalTests' source-file search, which already handles the common Java,
 * Kotlin, and Scala source roots. Keeping it behind this interface lets the backing implementation
 * change later without touching callers.
 */
@FunctionalInterface
interface TestSourceDirectoryLocator {

    /**
     * Returns the directory holding the test class's source file, with its package path included.
     *
     * @param testClass the test class whose source directory to find
     * @return the test's source directory
     * @throws IllegalStateException when the directory cannot be located
     */
    Path locate(Class<?> testClass);

    /**
     * The default locator, reusing ApprovalTests' {@code ClassUtils.find}. The search is rooted at
     * {@code src} rather than the working directory so that compiled or formatted mirror copies under
     * {@code build} cannot shadow the real sources. It returns the package directory of the test's
     * source file.
     *
     * @return a locator backed by ApprovalTests
     */
    static TestSourceDirectoryLocator approvalTests() {
        return testClass -> {
            try {
                // ApprovalTests locates the source file by the class's simple name, so a nested test class
                // has to resolve through its top-level enclosing class, which names the file on disk.
                var topLevel = testClass;
                while (topLevel.getEnclosingClass() != null) {
                    topLevel = topLevel.getEnclosingClass();
                }
                var segments = new ArrayList<>(Arrays.asList(topLevel.getName().split("\\.")));
                var last = segments.size() - 1;
                segments.set(last, segments.get(last) + ".java");
                var found = com.spun.util.ClassUtils.find(new File("src"), segments);
                return requireNonNull(found).getParentFile().toPath().normalize();
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Could not locate the test source directory for " + testClass.getName()
                                + ". Register a finder with org.approvaltests.TestUtils.registerSourceDirectoryFinder(...).",
                        e);
            }
        };
    }
}
