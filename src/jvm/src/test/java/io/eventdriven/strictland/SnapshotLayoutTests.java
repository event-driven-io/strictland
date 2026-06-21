package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLayoutTests {

    private static final String QUALIFIED_TYPE = "com.acme.orders.OrderPlaced";
    private static final String DOTLESS_TYPE = "OrderPlaced";
    private static final String SNAPSHOT_NAME = "withCoupon";
    private static final String EXTENSION = ".json";

    @Test
    void registry_usesTheDocumentedDefaults() {
        var layout = SnapshotLayout.registry();

        assertEquals("src/test/resources", layout.rootPath());
        assertEquals("contract-registry", layout.wrapperFolder());
    }

    @Test
    void withers_returnCopiesWithoutMutatingTheOriginal() {
        var original = SnapshotLayout.registry();

        var rerooted = original.rootPath("build/snaps");
        var wrapped = original.wrapperFolder("approved");

        assertEquals("build/snaps", rerooted.rootPath());
        assertEquals("approved", wrapped.wrapperFolder());
        assertEquals("src/test/resources", original.rootPath());
        assertEquals("contract-registry", original.wrapperFolder());
    }

    @Test
    void resolve_aQualifiedType_mirrorsItsNamespaceAsFoldersEndingInTheShortName() {
        var path = SnapshotLayout.registry().resolve(QUALIFIED_TYPE, SNAPSHOT_NAME, EXTENSION);

        assertEquals(
                Path.of(
                        "src/test/resources/contract-registry/com/acme/orders/OrderPlaced",
                        "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void resolve_aDotlessType_addsNoNamespaceFolders() {
        var path = SnapshotLayout.registry().resolve(DOTLESS_TYPE, SNAPSHOT_NAME, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/contract-registry/OrderPlaced", "withCoupon.snap.approved.json"), path);
    }

    @Test
    void resolve_rootPathOverride_rootsTheTreeElsewhere() {
        var path = SnapshotLayout.registry().rootPath("snaps").resolve(QUALIFIED_TYPE, SNAPSHOT_NAME, EXTENSION);

        assertEquals(
                Path.of("snaps/contract-registry/com/acme/orders/OrderPlaced", "withCoupon.snap.approved.json"), path);
    }

    @Test
    void resolve_wrapperFolderOverride_replacesTheWrapperSegment() {
        var path =
                SnapshotLayout.registry().wrapperFolder("approved").resolve(QUALIFIED_TYPE, SNAPSHOT_NAME, EXTENSION);

        assertEquals(
                Path.of("src/test/resources/approved/com/acme/orders/OrderPlaced", "withCoupon.snap.approved.json"),
                path);
    }

    @Test
    void resolve_honoursTheSerializerExtension() {
        var path = SnapshotLayout.registry().resolve(DOTLESS_TYPE, SNAPSHOT_NAME, ".csv");

        assertEquals(Path.of("src/test/resources/contract-registry/OrderPlaced", "withCoupon.snap.approved.csv"), path);
    }

    @Test
    void folder_aQualifiedType_mirrorsItsNamespaceEndingInTheShortName() {
        var folder = SnapshotLayout.registry().folder(MessageTypeName.of(QUALIFIED_TYPE));

        assertEquals(Path.of("src/test/resources/contract-registry/com/acme/orders/OrderPlaced"), folder);
    }

    @Test
    void folder_aDotlessType_addsNoNamespaceFolders() {
        var folder = SnapshotLayout.registry().folder(MessageTypeName.of(DOTLESS_TYPE));

        assertEquals(Path.of("src/test/resources/contract-registry/OrderPlaced"), folder);
    }
}
