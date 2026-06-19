package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class ManualVariantTests {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final Path VARIANTS_DIR =
            Path.of("src/test/java/io/eventdriven/strictland/snapshots/OrderInitiated");

    private record OrderInitiated(UUID orderId, String customer, String promotion) {}

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    private static SpecificationOptions options() {
        return Json.Jackson.defaults()
                .snapshotLayout(SnapshotLayout.nextToTest().grouping(SnapshotGrouping.PER_CONTRACT));
    }

    private static SpecificationOptions flatOptions() {
        return Json.Jackson.defaults()
                .snapshotLayout(SnapshotLayout.nextToTest()
                        .grouping(SnapshotGrouping.NONE)
                        .wrapperFolder(""));
    }

    @Test
    void twoVariantsOfOneType_writeTwoDistinctFiles_neitherOverwritingTheOther() {
        MessageContract.specification(options())
                .given(new OrderInitiated(ORDER_ID, "Alice", "WELCOME"))
                .whenSerializedAs(SnapshotVariant.named("WithPromotion"))
                .thenContractIsUnchanged();

        MessageContract.specification(options())
                .given(new OrderInitiated(ORDER_ID, "Alice", "NONE"))
                .whenSerializedAs(SnapshotVariant.named("NoPromotion"))
                .thenContractIsUnchanged();

        var withPromotion = VARIANTS_DIR.resolve(
                "OrderInitiated.1.ManualVariantTests.twoVariantsOfOneType_writeTwoDistinctFiles_neitherOverwritingTheOther.WithPromotion.snap.approved.json");
        var noPromotion = VARIANTS_DIR.resolve(
                "OrderInitiated.1.ManualVariantTests.twoVariantsOfOneType_writeTwoDistinctFiles_neitherOverwritingTheOther.NoPromotion.snap.approved.json");

        assertTrue(Files.exists(withPromotion), "expected snapshot at " + withPromotion);
        assertTrue(Files.exists(noPromotion), "expected snapshot at " + noPromotion);
        assertArrayEquals(
                "{\"orderId\":\"00000000-0000-0000-0000-000000000030\",\"customer\":\"Alice\",\"promotion\":\"WELCOME\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                readBytes(withPromotion));
        assertArrayEquals(
                "{\"orderId\":\"00000000-0000-0000-0000-000000000030\",\"customer\":\"Alice\",\"promotion\":\"NONE\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                readBytes(noPromotion));
    }

    @Test
    void aLabelledVariant_canBeReadBackByItsLabel() {
        MessageContract.specification(options())
                .given(new OrderInitiated(ORDER_ID, "Alice", "WELCOME"))
                .whenSerializedAs(SnapshotVariant.named("WithPromotion"))
                .thenContractIsUnchanged();

        MessageContract.specification(options())
                .given(Snapshot.of(OrderInitiated.class).variant("WithPromotion"))
                .whenDeserializedAs(OrderInitiated.class)
                .thenBackwardCompatible(order -> assertEquals("WELCOME", order.promotion()));
    }

    @Test
    void underFlat_theLabelNamesTheFlatApprovedFile() throws Exception {
        var snapshotFile = Path.of(
                "src/test/java/io/eventdriven/strictland/OrderInitiated.1.ManualVariantTests.underFlat_theLabelNamesTheFlatApprovedFile.FlatNullPromotion.snap.approved.json");
        try {
            MessageContract.specification(flatOptions())
                    .given(new OrderInitiated(ORDER_ID, "Alice", "NONE"))
                    .whenSerializedAs(SnapshotVariant.named("FlatNullPromotion"))
                    .thenContractIsUnchanged();

            assertTrue(Files.exists(snapshotFile), "expected the flat file at " + snapshotFile);

            // Read it back by label, with the source class recorded on the variant.
            MessageContract.specification(flatOptions())
                    .given(Snapshot.of(OrderInitiated.class).variant("FlatNullPromotion"))
                    .whenDeserializedAs(OrderInitiated.class)
                    .thenBackwardCompatible(order -> assertEquals("NONE", order.promotion()));
        } finally {
            Files.deleteIfExists(snapshotFile);
        }
    }

    @Test
    void theLabelIsRecordedAsTheLeafFileName() {
        MessageContract.specification(options())
                .given(new OrderInitiated(ORDER_ID, "Alice", "NONE"))
                .whenSerializedAs(SnapshotVariant.named("NoPromotion"))
                .thenContractIsUnchanged();

        // The label is the trailing segment of the leaf file name, so it reads as documentation.
        var leaf = VARIANTS_DIR.resolve(
                "OrderInitiated.1.ManualVariantTests.theLabelIsRecordedAsTheLeafFileName.NoPromotion.snap.approved.json");
        assertTrue(Files.exists(leaf), "expected the label to name the leaf at " + leaf);
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new AssertionError("could not read " + path, e);
        }
    }
}
