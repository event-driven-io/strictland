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
    void allKeysTogether_buildTheFullLayout() {
        var props = new Properties();
        props.setProperty("strictland.layout.rootPath", "src/test/resources");
        props.setProperty("strictland.layout.wrapperFolder", "approved");

        var layout = SnapshotLayoutProperties.fromProperties(props);

        assertEquals("src/test/resources", layout.rootPath());
        assertEquals("approved", layout.wrapperFolder());
    }

    @Test
    void fromClasspath_whenResourceIsPresent_parsesIt() {
        Optional<SnapshotLayout> layout = SnapshotLayoutProperties.fromClasspath("fixtures/layout-sample.properties");

        assertTrue(layout.isPresent());
        assertEquals("src/test/resources", layout.get().rootPath());
        assertEquals("approved", layout.get().wrapperFolder());
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
