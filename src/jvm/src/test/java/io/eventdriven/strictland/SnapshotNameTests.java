package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotNameTests {

    private static final String QUALIFIED_TYPE = "com.acme.orders.OrderPlaced";
    private static final String EXTENSION = ".json";

    @Test
    void base_withoutVariant_isShortNameDotVersion() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "1", null);

        assertEquals("OrderPlaced.1", name.base());
    }

    @Test
    void base_withBlankVariant_isShortNameDotVersion() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "1", "  ");

        assertEquals("OrderPlaced.1", name.base());
    }

    @Test
    void base_withVariant_appendsTheVariant() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "1", "withCoupon");

        assertEquals("OrderPlaced.1.withCoupon", name.base());
    }

    @Test
    void readPrefix_isShortNameDotVersionDot() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "2", "withCoupon");

        assertEquals("OrderPlaced.2.", name.readPrefix());
    }

    @Test
    void resolve_againstALayout_isTheFolderPlusTheApprovedFileName() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "1", "withCoupon");

        var path = name.resolve(SnapshotLayout.registry(), EXTENSION);

        assertEquals(
                Path.of(
                        "src/test/resources/contract-registry/com/acme/orders/OrderPlaced",
                        "OrderPlaced.1.withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void resolve_withoutVariant_dropsTheVariantSegment() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "1", null);

        var path = name.resolve(SnapshotLayout.registry(), EXTENSION);

        assertEquals(
                Path.of(
                        "src/test/resources/contract-registry/com/acme/orders/OrderPlaced",
                        "OrderPlaced.1.snap.approved.json"),
                path);
    }

    @Test
    void folder_isTheLayoutsFolderForTheMessageType() {
        var name = SnapshotName.of(QUALIFIED_TYPE, "1", null);

        assertEquals(
                Path.of("src/test/resources/contract-registry/com/acme/orders/OrderPlaced"),
                name.folder(SnapshotLayout.registry()));
    }

    @Test
    void base_servesAsTheKeyWhenThereIsNoLayout() {
        var name = SnapshotName.of("OrderPlaced", "1", "withCoupon");

        assertEquals("OrderPlaced.1.withCoupon", name.base());
    }
}
