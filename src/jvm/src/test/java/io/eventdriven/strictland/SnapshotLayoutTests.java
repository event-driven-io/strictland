package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals(SnapshotRoot.NEXT_TO_TEST, nextToTest.location());
        assertEquals(SnapshotGrouping.PER_TEST_CLASS, nextToTest.grouping());
        assertEquals("snapshots", nextToTest.wrapperFolder());
        assertEquals("", nextToTest.rootPath());
        assertEquals(TestClassNaming.SIMPLE, nextToTest.testClassNaming());

        var globalRoot = SnapshotLayout.globalRoot("src/test/resources/snapshots");
        assertEquals(SnapshotRoot.GLOBAL_ROOT, globalRoot.location());
        assertEquals(SnapshotGrouping.PER_TEST_CLASS, globalRoot.grouping());
        assertEquals("snapshots", globalRoot.wrapperFolder());
        assertEquals("src/test/resources/snapshots", globalRoot.rootPath());
        assertEquals(TestClassNaming.SIMPLE, globalRoot.testClassNaming());
    }

    @Test
    void withers_returnCopiesWithoutMutatingTheOriginal() {
        var original = SnapshotLayout.nextToTest();

        var grouped = original.grouping(SnapshotGrouping.PER_CONTRACT);
        var wrapped = original.wrapperFolder("approved");
        var qualified = original.testClassNaming(TestClassNaming.FULL);

        assertEquals(SnapshotGrouping.PER_CONTRACT, grouped.grouping());
        assertEquals("approved", wrapped.wrapperFolder());
        assertEquals(TestClassNaming.FULL, qualified.testClassNaming());
        assertEquals(SnapshotGrouping.PER_TEST_CLASS, original.grouping());
        assertEquals("snapshots", original.wrapperFolder());
        assertEquals(TestClassNaming.SIMPLE, original.testClassNaming());
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
                .grouping(SnapshotGrouping.PER_CONTRACT)
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderPlaced", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perContract_withVariant_usesTheVariantAsTheLeaf() {
        var path = SnapshotLayout.nextToTest()
                .grouping(SnapshotGrouping.PER_CONTRACT)
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
                .grouping(SnapshotGrouping.PER_CONTRACT)
                .resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/snapshots", PACKAGE_PATH, "OrderPlaced", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void globalRoot_perContract_withoutVariant_leafDefaultsToMessageType() {
        var path = SnapshotLayout.globalRoot("snaps")
                .grouping(SnapshotGrouping.PER_CONTRACT)
                .resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(Path.of("snaps", PACKAGE_PATH, "OrderPlaced", "OrderPlaced.snap.approved.json"), path);
    }

    @Test
    void nextToTest_noneGrouping_emptyWrapper_putsTheFileStraightInTheTestSourceDir() {
        var path = SnapshotLayout.nextToTest()
                .grouping(SnapshotGrouping.NONE)
                .wrapperFolder("")
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(TEST_SOURCE_DIR.resolve(MESSAGE_TYPE + ".snap.approved.json"), path);
    }

    @Test
    void nextToTest_noneGrouping_keepsTheWrapperButDropsTheGroupFolder() {
        var path = SnapshotLayout.nextToTest()
                .grouping(SnapshotGrouping.NONE)
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(Path.of("src/test/java", PACKAGE_PATH, "snapshots", "withCoupon.snap.approved.json"), path);
    }

    @Test
    void nextToTest_emptyWrapper_addsNoEmptySegment() {
        var path = SnapshotLayout.nextToTest()
                .wrapperFolder("")
                .resolve(TEST_SOURCE_DIR, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(Path.of("src/test/java", PACKAGE_PATH, "OrderTests", "OrderPlaced.snap.approved.json"), path);
    }

    @Test
    void globalRoot_noneGrouping_putsTheFileDirectlyUnderThePackagePath() {
        var path = SnapshotLayout.globalRoot("src/test/resources/snapshots")
                .grouping(SnapshotGrouping.NONE)
                .resolve(null, CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, MESSAGE_TYPE, EXTENSION);

        assertEquals(Path.of("src/test/resources/snapshots", PACKAGE_PATH, "OrderPlaced.snap.approved.json"), path);
    }
}
