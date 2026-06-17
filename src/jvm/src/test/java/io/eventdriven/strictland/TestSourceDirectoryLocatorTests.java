package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class TestSourceDirectoryLocatorTests {

    @Test
    void defaultLocator_findsThePackageDirectoryOfATestClass() {
        var dir = TestSourceDirectoryLocator.approvalTests().locate(TestSourceDirectoryLocatorTests.class);

        assertTrue(dir.endsWith("io/eventdriven/strictland"), dir.toString());
    }

    @Test
    void defaultLocator_whenSourceCannotBeFound_throwsAPointedError() {
        var ex = assertThrows(
                IllegalStateException.class,
                () -> TestSourceDirectoryLocator.approvalTests().locate(String.class));

        assertTrue(requireNonNull(ex.getMessage()).contains("registerSourceDirectoryFinder"));
    }
}
