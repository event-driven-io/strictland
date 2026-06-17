package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.eventdriven.strictland.SnapshotLayout.Strategy;
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
    void emptyProperties_fallBackToNextToTestDefaults() {
        var layout = SnapshotLayoutProperties.fromProperties(new Properties());

        assertEquals(SnapshotLayout.nextToTest(), layout);
    }

    @Test
    void strategyNextToTest_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "nextToTest");

        assertEquals(
                Strategy.NEXT_TO_TEST,
                SnapshotLayoutProperties.fromProperties(props).strategy());
    }

    @Test
    void strategyGlobalRoot_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "globalRoot");

        assertEquals(
                Strategy.GLOBAL_ROOT,
                SnapshotLayoutProperties.fromProperties(props).strategy());
    }

    @Test
    void strategyFlat_mapsToFlat() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "flat");

        assertEquals(
                Strategy.FLAT, SnapshotLayoutProperties.fromProperties(props).strategy());
    }

    @Test
    void strategyGlobalRoot_withoutRootPath_defaultsToEmptyRoot() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "globalRoot");

        var layout = SnapshotLayoutProperties.fromProperties(props);

        assertEquals(Strategy.GLOBAL_ROOT, layout.strategy());
        assertEquals("", layout.rootPath());
    }

    @Test
    void groupingPerTestClass_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.grouping", "perTestClass");

        assertEquals(
                Grouping.PER_TEST_CLASS,
                SnapshotLayoutProperties.fromProperties(props).grouping());
    }

    @Test
    void groupingPerContract_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.grouping", "perContract");

        assertEquals(
                Grouping.PER_CONTRACT,
                SnapshotLayoutProperties.fromProperties(props).grouping());
    }

    @Test
    void wrapperFolder_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.wrapperFolder", "approved");

        assertEquals("approved", SnapshotLayoutProperties.fromProperties(props).wrapperFolder());
    }

    @Test
    void rootPath_isParsed_whenStrategyIsGlobalRoot() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "globalRoot");
        props.setProperty("strictland.layout.rootPath", "snaps");

        assertEquals("snaps", SnapshotLayoutProperties.fromProperties(props).rootPath());
    }

    @Test
    void allKeysTogether_buildTheFullLayout() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "globalRoot");
        props.setProperty("strictland.layout.grouping", "perContract");
        props.setProperty("strictland.layout.wrapperFolder", "approved");
        props.setProperty("strictland.layout.rootPath", "src/test/resources/snapshots");

        var layout = SnapshotLayoutProperties.fromProperties(props);

        assertEquals(Strategy.GLOBAL_ROOT, layout.strategy());
        assertEquals(Grouping.PER_CONTRACT, layout.grouping());
        assertEquals("approved", layout.wrapperFolder());
        assertEquals("src/test/resources/snapshots", layout.rootPath());
    }

    @Test
    void unknownStrategy_throwsNamingTheKeyAndValue() {
        var props = new Properties();
        props.setProperty("strictland.layout.strategy", "sideways");

        var exception =
                assertThrows(IllegalArgumentException.class, () -> SnapshotLayoutProperties.fromProperties(props));
        var message = requireNonNull(exception.getMessage());
        assertTrue(message.contains("strictland.layout.strategy"));
        assertTrue(message.contains("sideways"));
    }

    @Test
    void unknownGrouping_throwsNamingTheKeyAndValue() {
        var props = new Properties();
        props.setProperty("strictland.layout.grouping", "byMood");

        var exception =
                assertThrows(IllegalArgumentException.class, () -> SnapshotLayoutProperties.fromProperties(props));
        var message = requireNonNull(exception.getMessage());
        assertTrue(message.contains("strictland.layout.grouping"));
        assertTrue(message.contains("byMood"));
    }

    @Test
    void fromClasspath_whenResourceIsPresent_parsesIt() {
        Optional<SnapshotLayout> layout = SnapshotLayoutProperties.fromClasspath("fixtures/layout-sample.properties");

        assertTrue(layout.isPresent());
        assertEquals(Strategy.GLOBAL_ROOT, layout.get().strategy());
        assertEquals(Grouping.PER_CONTRACT, layout.get().grouping());
        assertEquals("approved", layout.get().wrapperFolder());
        assertEquals("src/test/resources/snapshots", layout.get().rootPath());
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
