package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        var legacy = SnapshotLayout.legacy();
        assertEquals(Strategy.LEGACY, legacy.strategy());
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
        var path =
                SnapshotLayout.nextToTest().resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, null, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderTests", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perTestClass_withVariant_usesTheVariantAsTheLeaf() {
        var path = SnapshotLayout.nextToTest()
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderTests", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perContract_withoutVariant_groupsUnderTheMessageType() {
        var path = SnapshotLayout.nextToTest()
                .grouping(Grouping.PER_CONTRACT)
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, null, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderPlaced", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_perContract_withVariant_usesTheVariantAsTheLeaf() {
        var path = SnapshotLayout.nextToTest()
                .grouping(Grouping.PER_CONTRACT)
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "snapshots", "OrderPlaced", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void nextToTest_wrapperFolderOverride_replacesTheWrapperSegment() {
        var path = SnapshotLayout.nextToTest()
                .wrapperFolder("approved")
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, null, EXTENSION);

        assertEquals(
                Path.of("src/test/java", PACKAGE_PATH, "approved", "OrderTests", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void globalRoot_perTestClass_withoutVariant_rootsAtTheGivenRootPath() {
        var path = SnapshotLayout.globalRoot("src/test/resources/snapshots")
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, null, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/snapshots", PACKAGE_PATH, "OrderTests", "OrderPlaced.snap.approved.json"),
                path);
    }

    @Test
    void globalRoot_perContract_withVariant_rootsAtTheGivenRootPath() {
        var path = SnapshotLayout.globalRoot("src/test/resources/snapshots")
                .grouping(Grouping.PER_CONTRACT)
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/snapshots", PACKAGE_PATH, "OrderPlaced", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void globalRoot_perContract_withoutVariant_leafDefaultsToMessageType() {
        var path = SnapshotLayout.globalRoot("snaps")
                .grouping(Grouping.PER_CONTRACT)
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, null, EXTENSION);

        assertEquals(Path.of("snaps", PACKAGE_PATH, "OrderPlaced", "OrderPlaced.snap.approved.json"), path);
    }

    @Test
    void legacy_matchesTodaysFileSnapshotStoragePathShape() {
        var expected = todaysLegacyPath(MESSAGE_TYPE);

        var path = SnapshotLayout.legacy().resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, null, EXTENSION);

        assertEquals(expected, path);
    }

    @Test
    void legacy_ignoresExtensionAndWrapperAndGrouping_alwaysApprovedTxt() {
        var path = SnapshotLayout.legacy()
                .grouping(Grouping.PER_CONTRACT)
                .wrapperFolder("approved")
                .resolve(CALLER_PACKAGE, CALLER_SIMPLE_NAME, MESSAGE_TYPE, VARIANT, EXTENSION);

        assertEquals(Path.of("src/test/java", PACKAGE_PATH, "withCoupon.approved.txt"), path);
    }

    private static Path todaysLegacyPath(String snapshotName) {
        var packagePath = CALLER_PACKAGE.replace('.', '/');
        return Path.of("src/test/java", packagePath, snapshotName + ".approved.txt");
    }
}
