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
    void emptyProperties_fallBackToNextToTestDefaults() {
        var layout = SnapshotLayoutProperties.fromProperties(new Properties());

        assertEquals(SnapshotLayout.nextToTest(), layout);
    }

    @Test
    void locationNextToTest_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.location", "nextToTest");

        assertEquals(
                SnapshotRoot.NEXT_TO_TEST,
                SnapshotLayoutProperties.fromProperties(props).location());
    }

    @Test
    void locationGlobalRoot_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.location", "globalRoot");

        assertEquals(
                SnapshotRoot.GLOBAL_ROOT,
                SnapshotLayoutProperties.fromProperties(props).location());
    }

    @Test
    void locationGlobalRoot_withoutRootPath_defaultsToEmptyRoot() {
        var props = new Properties();
        props.setProperty("strictland.layout.location", "globalRoot");

        var layout = SnapshotLayoutProperties.fromProperties(props);

        assertEquals(SnapshotRoot.GLOBAL_ROOT, layout.location());
        assertEquals("", layout.rootPath());
    }

    @Test
    void groupingPerTestClass_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.grouping", "perTestClass");

        assertEquals(
                SnapshotGrouping.PER_TEST_CLASS,
                SnapshotLayoutProperties.fromProperties(props).grouping());
    }

    @Test
    void groupingPerContract_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.grouping", "perContract");

        assertEquals(
                SnapshotGrouping.PER_CONTRACT,
                SnapshotLayoutProperties.fromProperties(props).grouping());
    }

    @Test
    void groupingNone_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.grouping", "none");

        assertEquals(
                SnapshotGrouping.NONE,
                SnapshotLayoutProperties.fromProperties(props).grouping());
    }

    @Test
    void wrapperFolder_isParsed() {
        var props = new Properties();
        props.setProperty("strictland.layout.wrapperFolder", "approved");

        assertEquals("approved", SnapshotLayoutProperties.fromProperties(props).wrapperFolder());
    }

    @Test
    void rootPath_isParsed_whenLocationIsGlobalRoot() {
        var props = new Properties();
        props.setProperty("strictland.layout.location", "globalRoot");
        props.setProperty("strictland.layout.rootPath", "snaps");

        assertEquals("snaps", SnapshotLayoutProperties.fromProperties(props).rootPath());
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
        props.setProperty("strictland.layout.location", "globalRoot");
        props.setProperty("strictland.layout.grouping", "perContract");
        props.setProperty("strictland.layout.wrapperFolder", "approved");
        props.setProperty("strictland.layout.rootPath", "src/test/resources/snapshots");
        props.setProperty("strictland.layout.testClassNaming", "full");

        var layout = SnapshotLayoutProperties.fromProperties(props);

        assertEquals(SnapshotRoot.GLOBAL_ROOT, layout.location());
        assertEquals(SnapshotGrouping.PER_CONTRACT, layout.grouping());
        assertEquals("approved", layout.wrapperFolder());
        assertEquals("src/test/resources/snapshots", layout.rootPath());
        assertEquals(TestClassNaming.FULL, layout.testClassNaming());
    }

    @Test
    void unknownLocation_throwsNamingTheKeyAndValue() {
        var props = new Properties();
        props.setProperty("strictland.layout.location", "sideways");

        var exception =
                assertThrows(IllegalArgumentException.class, () -> SnapshotLayoutProperties.fromProperties(props));
        var message = requireNonNull(exception.getMessage());
        assertTrue(message.contains("strictland.layout.location"));
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
        assertEquals(SnapshotRoot.GLOBAL_ROOT, layout.get().location());
        assertEquals(SnapshotGrouping.PER_CONTRACT, layout.get().grouping());
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
