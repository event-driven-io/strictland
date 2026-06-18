package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class TestSourceDirectoryLocatorTests {

    private static final String PACKAGE = "io.eventdriven.strictland";
    private static final String PACKAGE_PATH = "io/eventdriven/strictland";

    @Test
    void defaultLocator_findsThePackageDirectoryUnderTheConventionalJavaRoot() {
        var dir = TestSourceDirectoryLocator.knownRoots().locate(PACKAGE, "TestSourceDirectoryLocatorTests.java");

        assertTrue(dir.endsWith("src/test/java/" + PACKAGE_PATH), dir.toString());
    }

    @Test
    void resolvesAKotlinSourceRoot_provingMultiLanguageSupport(@TempDir Path root) throws Exception {
        var packageDir = Files.createDirectories(root.resolve(PACKAGE_PATH));
        Files.writeString(packageDir.resolve("OrderTests.kt"), "// Kotlin test");

        var dir =
                TestSourceDirectoryLocator.knownRoots(List.of(root.toString())).locate(PACKAGE, "OrderTests.kt");

        assertEquals(packageDir.normalize(), dir);
    }

    @Test
    void checksRootsInOrder_andTakesTheFirstThatHoldsTheSource(@TempDir Path empty, @TempDir Path real)
            throws Exception {
        var packageDir = Files.createDirectories(real.resolve(PACKAGE_PATH));
        Files.writeString(packageDir.resolve("OrderTests.scala"), "// Scala test");

        var dir = TestSourceDirectoryLocator.knownRoots(List.of(empty.toString(), real.toString()))
                .locate(PACKAGE, "OrderTests.scala");

        assertEquals(packageDir.normalize(), dir);
    }

    @Test
    void withoutASourceFileName_fallsBackToTheFirstRootWhosePackageDirectoryExists(@TempDir Path root)
            throws Exception {
        var packageDir = Files.createDirectories(root.resolve(PACKAGE_PATH));

        var dir =
                TestSourceDirectoryLocator.knownRoots(List.of(root.toString())).locate(PACKAGE, null);

        assertEquals(packageDir.normalize(), dir);
    }

    @Test
    void whenNoRootHoldsTheSourceFile_throwsAPointedError(@TempDir Path root) {
        var ex = assertThrows(
                IllegalStateException.class,
                () -> TestSourceDirectoryLocator.knownRoots(List.of(root.toString()))
                        .locate(PACKAGE, "Missing.java"));

        var message = requireNonNull(ex.getMessage());
        assertTrue(message.contains("Missing.java"), message);
        assertTrue(message.contains("testSourceRoots"), message);
    }

    @Test
    void whenNoRootHoldsThePackageDirectory_throwsWithoutNamingASourceFile(@TempDir Path root) {
        var ex = assertThrows(
                IllegalStateException.class,
                () -> TestSourceDirectoryLocator.knownRoots(List.of(root.toString()))
                        .locate(PACKAGE, null));

        var message = requireNonNull(ex.getMessage());
        assertTrue(message.contains(PACKAGE), message);
        assertTrue(message.contains("testSourceRoots"), message);
    }
}
