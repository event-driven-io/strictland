package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class MisuseTests {

    private record OrderPlaced(UUID orderId, String customer, OffsetDateTime placedAt) {}

    private record Bad(Object payload) {}

    @Test
    void given_unserializableEvent_whenSerialized_thenFailsWithAWrappedError() {
        var bad = new Bad(new Object());

        assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(bad)
                        .whenSerialized()
                        .thenContractIsUnchanged());
    }

    @Test
    void given_unserializableEvent_whenDeserialized_thenFailsWithAWrappedError() {
        var bad = new Bad(new Object());

        var exception = assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(bad)
                        .whenDeserializedAs(Bad.class)
                        .thenForwardCompatible());

        assertTrue(requireNonNull(exception.getMessage()).contains("Serialization of Bad failed"));
    }

    @Test
    void given_messageTypeSnapshotSource_whenSerialized_thenFailsBecauseAnInstanceIsRequired() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.ofTypeNamed("OrderPlaced"))
                        .whenSerialized());

        assertTrue(requireNonNull(exception.getMessage()).contains("instance"));
    }

    @Test
    void given_classSnapshotSource_whenSerialized_thenFailsBecauseAnInstanceIsRequired() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.of(OrderPlaced.class))
                        .whenSerialized());

        assertTrue(requireNonNull(exception.getMessage()).contains("instance"));
    }

    @Test
    void given_pathSnapshotSource_whenSerialized_thenFailsBecauseAnInstanceIsRequired() {
        var path = Path.of("MisuseTests/Malformed.reference.snap.approved.json");

        var exception = assertThrows(
                IllegalStateException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.at(path))
                        .whenSerialized());

        assertTrue(requireNonNull(exception.getMessage()).contains("instance"));
    }

    @Test
    void given_malformedStoredSnapshot_whenDeserialized_thenFailsBecauseDeserializationFailed() {
        var path = Path.of("MisuseTests/Malformed.reference.snap.approved.json");

        var exception = assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.at(path))
                        .whenDeserializedAs(OrderPlaced.class)
                        .thenBackwardCompatible());

        var message = requireNonNull(exception.getMessage());
        assertTrue(message.contains("Deserialization") && message.contains("failed"));
    }

    @Test
    void given_storedSnapshotWithNullBody_whenDeserialized_thenFailsBecauseItDeserializedToEmpty() {
        var path = Path.of("MisuseTests/NullBody.reference.snap.approved.json");

        var exception = assertThrows(
                AssertionError.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.at(path))
                        .whenDeserializedAs(OrderPlaced.class)
                        .thenForwardCompatible());

        assertTrue(requireNonNull(exception.getMessage()).contains("returned empty"));
    }

    @Test
    void given_missingSnapshotFile_whenDeserialized_thenFailsBecauseFileCannotBeRead() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/DoesNotExist.approved.txt");

        var exception = assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(MessageSnapshot.at(path))
                        .whenDeserializedAs(OrderPlaced.class)
                        .thenBackwardCompatible());

        assertTrue(requireNonNull(exception.getMessage()).contains("Cannot read snapshot file"));
    }
}
