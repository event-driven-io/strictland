package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
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
                .given(MessageSnapshot.of(OrderInitiated.class).variant("WithPromotion"))
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

    @Test
    void anUnpinnedReference_replaysEveryVariantOfTheTypeAndVersion() {
        writeVariant("WithPromotion", "WELCOME", "1");
        writeVariant("NoPromotion", "NONE", "1");

        var promotionsSeen = new HashSet<String>();
        MessageContract.specification(options())
                .given(MessageSnapshot.of(OrderInitiated.class))
                .whenDeserializedAs(OrderInitiated.class)
                .thenBackwardCompatible(order -> promotionsSeen.add(order.promotion()));

        assertEquals(Set.of("WELCOME", "NONE"), promotionsSeen);
    }

    @Test
    void aPinnedVariant_readsOnlyThatVariantNotTheRestOfTheFamily() {
        writeVariant("WithPromotion", "WELCOME", "1");
        writeVariant("NoPromotion", "NONE", "1");

        var promotionsSeen = new HashSet<String>();
        MessageContract.specification(options())
                .given(MessageSnapshot.of(OrderInitiated.class).variant("WithPromotion"))
                .whenDeserializedAs(OrderInitiated.class)
                .thenBackwardCompatible(order -> promotionsSeen.add(order.promotion()));

        assertEquals(Set.of("WELCOME"), promotionsSeen);
    }

    @Test
    void aPinnedVariantAndVersion_readsOnlyThatVersionsVariant() {
        writeVariant("WithPromotion", "WELCOME", "1");
        writeVariant("WithPromotion", "WELCOME_V2", "2");

        var promotionsSeen = new HashSet<String>();
        MessageContract.specification(options())
                .given(MessageSnapshot.of(OrderInitiated.class)
                        .variant("WithPromotion")
                        .version("1"))
                .whenDeserializedAs(OrderInitiated.class)
                .thenBackwardCompatible(order -> promotionsSeen.add(order.promotion()));

        assertEquals(Set.of("WELCOME"), promotionsSeen);
    }

    private static void writeVariant(String label, String promotion, String version) {
        MessageContract.specification(options())
                .given(new OrderInitiated(ORDER_ID, "Alice", promotion), version)
                .whenSerializedAs(SnapshotVariant.named(label))
                .thenContractIsUnchanged();
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new AssertionError("could not read " + path, e);
        }
    }
}
