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
            Path.of("src/test/resources/contract-registry/io/eventdriven/strictland/ManualVariantTests/OrderInitiated");

    private record OrderInitiated(UUID orderId, String customer, String promotion) {}

    @AfterEach
    void resetGlobalDefaults() {
        Strictland.resetDefaults();
    }

    private static SpecificationOptions options() {
        return Json.Jackson.defaults().snapshotLayout(SnapshotLayout.registry());
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

        var withPromotion = VARIANTS_DIR.resolve("OrderInitiated.1.WithPromotion.snap.approved.json");
        var noPromotion = VARIANTS_DIR.resolve("OrderInitiated.1.NoPromotion.snap.approved.json");

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
    void theLabelIsRecordedAsTheLeafFileName() {
        MessageContract.specification(options())
                .given(new OrderInitiated(ORDER_ID, "Alice", "NONE"))
                .whenSerializedAs(SnapshotVariant.named("NoPromotion"))
                .thenContractIsUnchanged();

        // The label is the trailing segment of the snapshot's file name, so it reads as documentation.
        var leaf = VARIANTS_DIR.resolve("OrderInitiated.1.NoPromotion.snap.approved.json");
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
