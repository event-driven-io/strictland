package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.eventdriven.strictland.SnapshotLayout.Strategy;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLayoutTests {
    private static final String CALLER_PACKAGE = "io.eventdriven.strictland.tests.contracts.v1";
    private static final String CALLER_SIMPLE_NAME = "OrderTests";
    private static final String MESSAGE_TYPE = "OrderPlaced";
    private static final String VARIANT = "withCoupon";
    private static final String EXTENSION = ".json";
    private static final String PACKAGE_PATH = "io/eventdriven/strictland/tests/contracts/v1";
    private static final Path TEST_SOURCE_DIR = Path.of("src/test/java", PACKAGE_PATH);

    @Test
    void factories_useTheDocumentedDefaults() {
        var nextToTest = SnapshotLayout.nextToTest();
        assertEquals(Strategy.NEXT_TO_TEST, nextToTest.strategy());
        assertEquals(Grouping.PER_TEST_CLASS, nextToTest.grouping());
        assertEquals("snapshots", nextToTest.wrapperFolder());
        assertEquals("", nextToTest.rootPath());

        var globalRoot = SnapshotLayout.globalRoot("src/test/resources/snapshots");
        assertEquals(Strategy.GLOBAL_ROOT, globalRoot.strategy());
        assertEquals(Grouping.PER_TEST_CLASS, globalRoot.grouping());
        assertEquals("snapshots", globalRoot.wrapperFolder());
        assertEquals("src/test/resources/snapshots", globalRoot.rootPath());

        var flat = SnapshotLayout.flat();
        assertEquals(Strategy.FLAT, flat.strategy());
    }

    @Test
    void withers_returnCopiesWithoutMutatingTheOriginal() {
        var original = SnapshotLayout.nextToTest();

        var grouped = original.grouping(Grouping.PER_CONTRACT);
        var wrapped = original.wrapperFolder("approved");

        assertEquals(Grouping.PER_CONTRACT, grouped.grouping());
        assertEquals("approved", wrapped.wrapperFolder());
        assertEquals(Grouping.PER_TEST_CLASS, original.grouping());
        assertEquals("snapshots", original.wrapperFolder());
    }

    @Test
    void nextToTest_perTestClass_withoutVariant_groupsUnderTheTestClass() {
        var path = SnapshotLayout.nextToTest()
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderTests", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perTestClass_withVariant_usesTheVariantAsTheLeaf() {
        var path = SnapshotLayout.nextToTest()
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderTests", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perContract_withoutVariant_groupsUnderTheMessageType() {
        var path = SnapshotLayout.nextToTest()
                .grouping(Grouping.PER_CONTRACT)
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderPlaced", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perContract_withVariant_usesTheVariantAsTheLeaf() {
        var path = SnapshotLayout.nextToTest()
                .grouping(Grouping.PER_CONTRACT)
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderPlaced", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_wrapperFolderOverride_replacesTheWrapperSegment() {
        var path = SnapshotLayout.nextToTest()
                .wrapperFolder("approved")
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "approved", "OrderTests", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_withoutATestSourceDir_isRejected() {
        var layout = SnapshotLayout.nextToTest();

        assertThrows(
                IllegalArgumentException.class,
                () -> layout.resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION));
    }

    @Test
    void globalRoot_perTestClass_withoutVariant_rootsAtTheGivenRootPath() {
        var path = SnapshotLayout.globalRoot("src/test/resources/snapshots")
                .resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/snapshots", PACKAGE_PATH, "OrderTests", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void globalRoot_perContract_withVariant_rootsAtTheGivenRootPath() {
        var path = SnapshotLayout.globalRoot("src/test/resources/snapshots")
                .grouping(Grouping.PER_CONTRACT)
                .resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/snapshots", PACKAGE_PATH, "OrderPlaced", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void globalRoot_perContract_withoutVariant_leafDefaultsToMessageType() {
        var path = SnapshotLayout.globalRoot("snaps")
                .grouping(Grouping.PER_CONTRACT)
                .resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(Path.of("snaps", PACKAGE_PATH, "OrderPlaced", "OrderPlaced.snap.approved.json"), path);
    }

    @Test
    void flat_matchesTodaysFileSnapshotStoragePathShape() {
        var expected = TEST_SOURCE_DIR.resolve(MESSAGE_TYPE + ".approved.txt");

        var path = SnapshotLayout.flat()
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(expected, path);
    }

    @Test
    void flat_ignoresExtensionAndWrapperAndGrouping_alwaysApprovedTxt() {
        var path = SnapshotLayout.flat()
                .grouping(Grouping.PER_CONTRACT)
                .wrapperFolder("approved")
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(TEST_SOURCE_DIR.resolve("withCoupon.approved.txt"), path);
    }

    @Test
    void flat_withoutATestSourceDir_isRejected() {
        var layout = SnapshotLayout.flat();

        assertThrows(
                IllegalArgumentException.class,
                () -> layout.resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION));
    }
}
