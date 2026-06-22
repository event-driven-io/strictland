package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;

/**
 * Verifies a rendered public-API surface against an approved file committed next to the test, the way
 * {@link PublicApiContractTests} pins Strictland's own surface. It dogfoods Strictland's own pieces -
 * {@link TestSourceDirectoryLocator} to find the test's source directory and {@link
 * FileSnapshotStorage} to write the first run and compare later ones - so the suite needs no external
 * approval tool. The approved file is named {@code <TestClass>.<testMethod>.snap.approved.txt}.
 */
@NullMarked
final class ApiSurfaceApproval {

    private ApiSurfaceApproval() {}

    static void verify(String surface) {
        var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(1).findFirst())
                .orElseThrow(() -> new IllegalStateException("Cannot determine the calling test from the stack"));
        var testClass = caller.getDeclaringClass();
        var sourceDir =
                TestSourceDirectoryLocator.knownRoots().locate(testClass.getPackageName(), caller.getFileName());
        var fileName = testClass.getSimpleName() + "." + caller.getMethodName() + ".snap.approved.txt";
        var storage = new FileSnapshotStorage(
                SnapshotLayout.registry().rootPath(sourceDir.toString()).wrapperFolder(""));
        var location = SnapshotLocation.ofRelativePath(Path.of(fileName));
        storage.store(location, new SnapshotData(surface.getBytes(UTF_8)));
    }
}
