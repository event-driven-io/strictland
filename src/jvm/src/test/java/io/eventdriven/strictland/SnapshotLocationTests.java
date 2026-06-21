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
    private static final String MESSAGE_TYPE = "com.acme.orders.OrderPlaced";

    private static Path registryFolder(Path root) {
        return root.resolve("contract-snapshots").resolve("com/acme/orders/OrderPlaced");
    }

    @Test
    void resolveForWrite_simpleNaming_composesTheOwnedBaseNameWithoutVariant(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.registry().rootPath(root.toString()), ".json");

        var key = location.resolveForWrite(MESSAGE_TYPE, "1", null);

        var expected = registryFolder(root)
                .resolve("OrderPlaced.1." + THIS_CLASS_SIMPLE
                        + ".resolveForWrite_simpleNaming_composesTheOwnedBaseNameWithoutVariant.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_withVariant_appendsTheVariantToTheDiscriminator(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.registry().rootPath(root.toString()), ".json");

        var key = location.resolveForWrite(MESSAGE_TYPE, "1", "withCoupon");

        var expected = registryFolder(root)
                .resolve(
                        "OrderPlaced.1." + THIS_CLASS_SIMPLE
                                + ".resolveForWrite_withVariant_appendsTheVariantToTheDiscriminator.withCoupon.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_versionOverride_appearsInTheBaseName(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.registry().rootPath(root.toString()), ".json");

        var key = location.resolveForWrite(MESSAGE_TYPE, "2", null);

        var expected = registryFolder(root)
                .resolve("OrderPlaced.2." + THIS_CLASS_SIMPLE
                        + ".resolveForWrite_versionOverride_appearsInTheBaseName.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_fullNaming_usesTheFullyQualifiedClassInTheDiscriminator(@TempDir Path root) {
        var location = new SnapshotLocation(
                SnapshotLayout.registry().rootPath(root.toString()).testClassNaming(TestClassNaming.FULL), ".json");

        var key = location.resolveForWrite(MESSAGE_TYPE, "1", null);

        // The folder mirrors the message type; only the test-class segment of the variant is qualified.
        var expected = registryFolder(root)
                .resolve(
                        "OrderPlaced.1." + THIS_CLASS_FULL
                                + ".resolveForWrite_fullNaming_usesTheFullyQualifiedClassInTheDiscriminator.snap.approved.json");
        assertEquals(expected.toString(), key);
    }

    @Test
    void resolveForWrite_customStorage_returnsTheBareBaseName() {
        var location = new SnapshotLocation(null, ".json", TestClassNaming.SIMPLE);

        var key = location.resolveForWrite(MESSAGE_TYPE, "1", null);

        assertEquals(
                "OrderPlaced.1." + THIS_CLASS_SIMPLE + ".resolveForWrite_customStorage_returnsTheBareBaseName", key);
    }

    @Test
    void resolveForRead_globsTheContractVersionPrefixInTheRegistryFolder(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.registry().rootPath(root.toString()), ".json");

        var readLocation = location.resolveForRead(MESSAGE_TYPE, "1", null);

        assertEquals(registryFolder(root), readLocation.folder());
        assertEquals("OrderPlaced.1.", readLocation.prefix());
    }

    @Test
    void resolveForRead_aDotlessType_globsUnderTheShortNameFolderWithNoNamespace(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.registry().rootPath(root.toString()), ".json");

        var readLocation = location.resolveForRead("OrderPlaced", "1", null);

        assertEquals(root.resolve("contract-snapshots").resolve("OrderPlaced"), readLocation.folder());
        assertEquals("OrderPlaced.1.", readLocation.prefix());
    }

    @Test
    void resolveForRead_withVariant_carriesTheVariantLabel(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.registry().rootPath(root.toString()), ".json");

        var readLocation = location.resolveForRead(MESSAGE_TYPE, "1", "Label");

        assertEquals("Label", readLocation.variant());
    }

    @Test
    void resolveForRead_customStorage_carriesNoFolderAndJustThePrefix() {
        var location = new SnapshotLocation(null, ".json", TestClassNaming.SIMPLE);

        var readLocation = location.resolveForRead(MESSAGE_TYPE, "1", null);

        assertNull(readLocation.folder());
        assertEquals("OrderPlaced.1.", readLocation.prefix());
    }
}
