package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLocationTests {

    private static final MessageTypeName QUALIFIED_TYPE = MessageTypeName.of("com.acme.orders.OrderPlaced");
    private static final MessageTypeName DOTLESS_TYPE = MessageTypeName.of("OrderPlaced");
    private static final String EXTENSION = ".json";

    @Test
    void of_aQualifiedType_mirrorsTheNamespaceAsFoldersEndingInTheShortName() {
        var location = SnapshotLocation.of(QUALIFIED_TYPE, "1", SnapshotVariant.named("withCoupon"), EXTENSION);

        assertEquals(List.of("com", "acme", "orders", "OrderPlaced"), location.path());
    }

    @Test
    void of_aDotlessType_hasNoNamespaceFolders() {
        var location = SnapshotLocation.of(DOTLESS_TYPE, "1", SnapshotVariant.named("withCoupon"), EXTENSION);

        assertEquals(List.of("OrderPlaced"), location.path());
    }

    @Test
    void of_withAVariant_namesTheStemShortNameDotVersionDotVariant() {
        var location = SnapshotLocation.of(QUALIFIED_TYPE, "2", SnapshotVariant.named("withCoupon"), EXTENSION);

        assertEquals("OrderPlaced.2.withCoupon", location.name());
    }

    @Test
    void of_withTheUnsetVariant_rendersTheDefaultLabel() {
        var location = SnapshotLocation.of(QUALIFIED_TYPE, "1", SnapshotVariant.UNSET, EXTENSION);

        assertEquals("OrderPlaced.1.default", location.name());
    }

    @Test
    void of_withTheDefaultVariant_rendersTheDefaultLabel() {
        var location = SnapshotLocation.of(QUALIFIED_TYPE, "1", SnapshotVariant.DEFAULT, EXTENSION);

        assertEquals("OrderPlaced.1.default", location.name());
    }

    @Test
    void of_carriesTheSerializerExtension() {
        var location = SnapshotLocation.of(DOTLESS_TYPE, "1", SnapshotVariant.UNSET, ".csv");

        assertEquals(".csv", location.extension());
    }

    @Test
    void ofRelativePath_decomposesTheFolderStemAndExtension() {
        var location = SnapshotLocation.ofRelativePath(
                Path.of("com/acme/orders/OrderPlaced", "OrderPlaced.1.withCoupon.snap.approved.json"));

        assertEquals(List.of("com", "acme", "orders", "OrderPlaced"), location.path());
        assertEquals("OrderPlaced.1.withCoupon", location.name());
        assertEquals(".json", location.extension());
    }

    @Test
    void ofRelativePath_aBareFileNameHasNoFolderSegments() {
        var location = SnapshotLocation.ofRelativePath(Path.of("OrderPlaced.1.default.snap.approved.json"));

        assertEquals(List.of(), location.path());
        assertEquals("OrderPlaced.1.default", location.name());
        assertEquals(".json", location.extension());
    }

    @Test
    void ofRelativePath_withoutTheApprovedMarker_splitsOnTheLastDot() {
        var location = SnapshotLocation.ofRelativePath(Path.of("snaps", "Direct.json"));

        assertEquals(List.of("snaps"), location.path());
        assertEquals("Direct", location.name());
        assertEquals(".json", location.extension());
    }

    @Test
    void ofRelativePath_aDotlessFileNameHasAnEmptyExtension() {
        var location = SnapshotLocation.ofRelativePath(Path.of("Direct"));

        assertEquals(List.of(), location.path());
        assertEquals("Direct", location.name());
        assertEquals("", location.extension());
    }
}
