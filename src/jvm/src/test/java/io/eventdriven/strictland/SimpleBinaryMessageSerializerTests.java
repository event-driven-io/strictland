package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class SimpleBinaryMessageSerializerTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Path SNAPSHOT_DIR = Path.of("src/test/java/io/eventdriven/strictland/snapshots");

    private interface Binary {
        static SpecificationOptions defaults() {
            return of(SNAPSHOT_DIR);
        }

        static SpecificationOptions of(Path dir) {
            return SpecificationOptions.serializer(new SimpleBinaryMessageSerializer())
                    .snapshotStorage(new Base64SnapshotStorage(dir));
        }
    }

    private record OrderPlaced(UUID orderId, String customer) {}

    private record OrderPlacedNullable(
            UUID orderId, @Nullable String customer) {}

    @Test
    void givenBinarySerializerAndCustomStorage_whenSerialized_thenContractIsUnchanged() {
        MessageContract.specification(Binary.defaults())
                .given(new OrderPlaced(FIXED_ID, "Alice"))
                .whenSerializedAs(Snapshot.forMessageType("OrderPlaced"))
                .thenContractIsUnchanged();
    }

    @Test
    void givenCustomStorageAndAVariant_whenSerialized_theVariantKeysTheSnapshot(@TempDir Path tmp) {
        MessageContract.specification(Binary.of(tmp))
                .given(new OrderPlaced(FIXED_ID, "Alice"))
                .whenSerializedAs(SnapshotVariant.named("AliceVariant"))
                .thenContractIsUnchanged();

        assertTrue(java.nio.file.Files.exists(tmp.resolve("OrderPlaced.1.AliceVariant.approved.txt")));
    }

    @Test
    void givenCustomStorage_whenReadingASnapshotBackByType_thenItResolvesTheLeafKey(@TempDir Path tmp) {
        var options = Binary.of(tmp);
        MessageContract.specification(options)
                .given(new OrderPlaced(FIXED_ID, "Alice"))
                .whenSerializedAs(Snapshot.forMessageType("OrderPlaced"))
                .thenContractIsUnchanged();

        MessageContract.specification(options)
                .given(Snapshot.forMessageType("OrderPlaced"))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }

    @Test
    void givenCustomStorage_whenReadingASnapshotBackByTypeAndVariant_thenItResolvesTheVariant(@TempDir Path tmp) {
        var options = Binary.of(tmp);
        MessageContract.specification(options)
                .given(new OrderPlaced(FIXED_ID, "Alice"))
                .whenSerializedAs(Snapshot.forMessageType("OrderPlaced").variant("Alice"))
                .thenContractIsUnchanged();

        MessageContract.specification(options)
                .given(Snapshot.forMessageType("OrderPlaced").variant("Alice"))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));
    }

    @Test
    void givenBinaryBytes_whenStoredAndReadBack_thenRecordRoundTripsThroughBase64(@TempDir Path tmp) {
        var serializer = new SimpleBinaryMessageSerializer();
        var storage = new Base64SnapshotStorage(tmp);
        var original = new OrderPlaced(FIXED_ID, "Alice");

        storage.store("RoundTrip", serializer.serialize(original));

        var bytes = storage.read("RoundTrip").orElseThrow();
        var roundTripped = serializer.deserialize(bytes, OrderPlaced.class);

        assertEquals(original, roundTripped);
    }

    @Test
    void givenBytesWithoutTheMagicHeader_whenDeserialized_thenThrows() {
        var serializer = new SimpleBinaryMessageSerializer();
        var bytes = "not a binary snapshot".getBytes(UTF_8);

        var error =
                assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(bytes, OrderPlaced.class));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("bad magic header"));
    }

    @Test
    void givenBytesWithAMismatchedComponentCount_whenDeserialized_thenThrows() {
        var serializer = new SimpleBinaryMessageSerializer();
        var bytes = serializer.serialize(new OrderPlacedNullable(FIXED_ID, "Alice"));

        var error = assertThrows(
                IllegalArgumentException.class, () -> serializer.deserialize(bytes, OrderPlacedThreeFields.class));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("Component count mismatch"));
    }

    @Test
    void givenARecordWithANullComponent_whenSerialized_thenThrows() {
        var serializer = new SimpleBinaryMessageSerializer();
        var event = new OrderPlacedNullable(FIXED_ID, null);

        var error = assertThrows(IllegalArgumentException.class, () -> serializer.serialize(event));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("Null component not supported"));
    }

    @Test
    void givenAnApprovedSnapshot_whenStoringADifferentPayload_thenThrowsDrift(@TempDir Path tmp) {
        var serializer = new SimpleBinaryMessageSerializer();
        var storage = new Base64SnapshotStorage(tmp);
        storage.store("Drifting", serializer.serialize(new OrderPlaced(FIXED_ID, "Alice")));

        var error = assertThrows(
                AssertionError.class,
                () -> storage.store("Drifting", serializer.serialize(new OrderPlaced(FIXED_ID, "Bob"))));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("drift"));
    }

    @Test
    void givenAnAbsentSnapshotName_whenRead_thenEmpty(@TempDir Path tmp) {
        var storage = new Base64SnapshotStorage(tmp);

        assertTrue(storage.read("Missing").isEmpty());
    }

    private record OrderPlacedThreeFields(UUID orderId, String customer, String currency) {}
}
