package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class SnapshotLocationTests {

    private static final String THIS_CLASS_SIMPLE = "SnapshotLocationTests";
    private static final String THIS_CLASS_FULL = SnapshotLocationTests.class.getName();

    private static SnapshotLocation anchoredAt(Path src, SnapshotLayout layout) {
        return new SnapshotLocation(layout, ".json", layout.testClassNaming(), (packageName, sourceFileName) -> src);
    }

    @Test
    void resolveForWrite_simpleNaming_composesTheOwnedBaseNameWithoutVariant(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest());

        var key = location.resolveForWrite("OrderPlaced", "1", null);

        var expected = src.resolve("snapshots")
                .resolve(THIS_CLASS_SIMPLE)
                .resolve("OrderPlaced.1." + THIS_CLASS_SIMPLE
                        + ".resolveForWrite_simpleNaming_composesTheOwnedBaseNameWithoutVariant.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_withVariant_appendsTheVariantToTheDiscriminator(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest());

        var key = location.resolveForWrite("OrderPlaced", "1", "withCoupon");

        var expected = src.resolve("snapshots")
                .resolve(THIS_CLASS_SIMPLE)
                .resolve(
                        "OrderPlaced.1." + THIS_CLASS_SIMPLE
                                + ".resolveForWrite_withVariant_appendsTheVariantToTheDiscriminator.withCoupon.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_versionOverride_appearsInTheBaseName(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest());

        var key = location.resolveForWrite("OrderPlaced", "2", null);

        var expected = src.resolve("snapshots")
                .resolve(THIS_CLASS_SIMPLE)
                .resolve("OrderPlaced.2." + THIS_CLASS_SIMPLE
                        + ".resolveForWrite_versionOverride_appearsInTheBaseName.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_fullNaming_usesTheFullyQualifiedClassInTheDiscriminator(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest().testClassNaming(TestClassNaming.FULL));

        var key = location.resolveForWrite("OrderPlaced", "1", null);

        // The folder grouping still uses the simple class name; only the discriminator is qualified.
        var expected = src.resolve("snapshots")
                .resolve(THIS_CLASS_SIMPLE)
                .resolve(
                        "OrderPlaced.1." + THIS_CLASS_FULL
                                + ".resolveForWrite_fullNaming_usesTheFullyQualifiedClassInTheDiscriminator.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_customStorage_returnsTheBareBaseName() {
        var location = new SnapshotLocation(
                null, ".json", TestClassNaming.SIMPLE, (packageName, sourceFileName) -> Path.of("ignored"));

        var key = location.resolveForWrite("OrderPlaced", "1", null);

        assertEquals(
                "OrderPlaced.1." + THIS_CLASS_SIMPLE + ".resolveForWrite_customStorage_returnsTheBareBaseName", key);
    }

    @Test
    void resolveForRead_globsTheContractVersionPrefixInTheGroupFolder(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest());

        var readLocation = location.resolveForRead("OrderPlaced", "1", null);

        assertEquals(src.resolve("snapshots").resolve(THIS_CLASS_SIMPLE), readLocation.folder());
        assertEquals("OrderPlaced.1.", readLocation.prefix());
    }

    @Test
    void resolveForRead_withVariant_carriesTheVariantLabel(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest());

        var readLocation = location.resolveForRead("OrderPlaced", "1", "Label");

        assertEquals("Label", readLocation.variant());
    }

    @Test
    void resolveForRead_customStorage_carriesNoFolderAndJustThePrefix() {
        var location = new SnapshotLocation(
                null, ".json", TestClassNaming.SIMPLE, (packageName, sourceFileName) -> Path.of("ignored"));

        var readLocation = location.resolveForRead("OrderPlaced", "1", null);

        assertNull(readLocation.folder());
        assertEquals("OrderPlaced.1.", readLocation.prefix());
    }

    @Test
    void resolveForRead_perContract_globsUnderTheMessageTypeFolder(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest().grouping(SnapshotGrouping.PER_CONTRACT));

        var readLocation = location.resolveForRead("OrderPlaced", "1", null);

        assertEquals(src.resolve("snapshots").resolve("OrderPlaced"), readLocation.folder());
        assertEquals("OrderPlaced.1.", readLocation.prefix());
    }
}
