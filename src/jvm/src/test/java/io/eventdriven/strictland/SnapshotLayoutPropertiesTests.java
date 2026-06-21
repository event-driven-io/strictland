package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLayoutPropertiesTests {

    @Test
    void emptyProperties_fallBackToRegistryDefaults() {
        var layout = SnapshotLayoutProperties.fromProperties(new Properties());

        assertEquals(SnapshotLayout.registry(), layout);
    }

    @Test
    void rootPath_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.rootPath", "snaps");

        assertEquals("snaps", SnapshotLayoutProperties.fromProperties(props).rootPath());
    }

    @Test
    void wrapperFolder_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.wrapperFolder", "approved");

        assertEquals("approved", SnapshotLayoutProperties.fromProperties(props).wrapperFolder());
    }

    @Test
    void testClassNamingSimple_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.testClassNaming", "simple");

        assertEquals(
                TestClassNaming.SIMPLE,
                SnapshotLayoutProperties.fromProperties(props).testClassNaming());
    }

    @Test
    void testClassNamingFull_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.testClassNaming", "full");

        assertEquals(
                TestClassNaming.FULL,
                SnapshotLayoutProperties.fromProperties(props).testClassNaming());
    }

    @Test
    void unknownTestClassNaming_throwsNamingTheKeyAndValue() {
        var props = new Properties();
        props.setProperty("strictland.layout.testClassNaming", "sideways");

        var exception =
                assertThrows(IllegalArgumentException.class, () -> SnapshotLayoutProperties.fromProperties(props));
        var message = requireNonNull(exception.getMessage());
        assertTrue(message.contains("strictland.layout.testClassNaming"));
        assertTrue(message.contains("sideways"));
    }

    @Test
    void allKeysTogether_buildTheFullLayout() {
        var props = new Properties();
        props.setProperty("strictland.layout.rootPath", "src/test/resources");
        props.setProperty("strictland.layout.wrapperFolder", "approved");
        props.setProperty("strictland.layout.testClassNaming", "full");

        var layout = SnapshotLayoutProperties.fromProperties(props);

        assertEquals("src/test/resources", layout.rootPath());
        assertEquals("approved", layout.wrapperFolder());
        assertEquals(TestClassNaming.FULL, layout.testClassNaming());
    }

    @Test
    void fromClasspath_whenResourceIsPresent_parsesIt() {
        Optional<SnapshotLayout> layout = SnapshotLayoutProperties.fromClasspath("fixtures/layout-sample.properties");

        assertTrue(layout.isPresent());
        assertEquals("src/test/resources", layout.get().rootPath());
        assertEquals("approved", layout.get().wrapperFolder());
        assertEquals(TestClassNaming.SIMPLE, layout.get().testClassNaming());
    }

    @Test
    void fromClasspath_whenResourceIsAbsent_returnsEmpty() {
        assertEquals(Optional.empty(), SnapshotLayoutProperties.fromClasspath());
    }

    @Test
    void fromStream_whenStreamIsNull_returnsEmpty() {
        assertEquals(Optional.empty(), SnapshotLayoutProperties.fromStream("nowhere", null));
    }

    @Test
    void fromStream_whenReadingFails_wrapsTheIoException() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                throw new IOException("boom");
            }
        };

        var exception =
                assertThrows(UncheckedIOException.class, () -> SnapshotLayoutProperties.fromStream("failing", failing));
        var message = requireNonNull(exception.getMessage());
        assertTrue(message.contains("failing"));
    }
}
