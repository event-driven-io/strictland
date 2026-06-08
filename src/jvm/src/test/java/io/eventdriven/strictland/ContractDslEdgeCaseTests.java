package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

final class ContractDslEdgeCaseTests {
    private static final UUID FIXED_CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);
    private static final ObjectMapper CUSTOM_MAPPER = new JsonMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    record ShoppingCartConfirmed(
            UUID shoppingCartId, @Nullable String clientId, OffsetDateTime confirmedAt) {}

    record EmptyEvent() {}

    record Unserializable(UUID id) {
        @Override
        @SuppressWarnings("DoNotCallSuggester")
        public UUID id() {
            throw new RuntimeException("boom");
        }
    }

    @Test
    void specification_withCustomMapper_isUsedForSerialization() {
        Contract.specification(CUSTOM_MAPPER)
                .given(new ShoppingCartConfirmed(FIXED_CART_ID, "anonymised", FIXED_DATE))
                .whenSerialized()
                .thenContractIsUnchanged();
    }

    @Test
    void given_byMessageType_whenSerialized_verifiesAgainstNamedSnapshot() {
        Contract.specification()
                .given(new ShoppingCartConfirmed(FIXED_CART_ID, "anonymised", FIXED_DATE))
                .whenSerialized(
                        Snapshot.forMessageType("ShoppingCartConfirmed_ByMessageType", ShoppingCartConfirmed.class))
                .thenContractIsUnchanged();
    }

    @Test
    void given_byPath_whenSerialized_verifiesAgainstSnapshotAtPath() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/ShoppingCartConfirmed_ByPath.approved.txt");

        Contract.specification()
                .given(new ShoppingCartConfirmed(FIXED_CART_ID, "anonymised", FIXED_DATE))
                .whenSerialized(Snapshot.at(path, ShoppingCartConfirmed.class))
                .thenContractIsUnchanged();
    }

    @Test
    void given_byPath_withRelativeSinglePartPath_throwsBecausePathHasNoParent() {
        var path = Path.of("Orphan.approved.txt");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> Contract.specification()
                        .given(new ShoppingCartConfirmed(FIXED_CART_ID, "anonymised", FIXED_DATE))
                        .whenSerialized(Snapshot.at(path))
                        .thenContractIsUnchanged());

        assertTrue(requireNonNull(exception.getMessage()).contains("must have a parent directory"));
    }

    @Test
    void given_snapshotByMessageType_whenSerialized_throwsBecauseInstanceIsRequired() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> Contract.specification()
                        .given(Snapshot.forMessageType("ShoppingCartConfirmed"))
                        .whenSerialized());

        assertTrue(requireNonNull(exception.getMessage()).contains("Contract.given(instance)"));
    }

    @Test
    void given_snapshotByPath_whenSerialized_throwsBecauseInstanceIsRequired() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/ShoppingCartConfirmed.approved.txt");

        var exception = assertThrows(
                IllegalStateException.class,
                () -> Contract.specification().given(Snapshot.at(path)).whenSerialized());

        assertTrue(requireNonNull(exception.getMessage()).contains("Contract.given(instance)"));
    }

    @Test
    void given_snapshotByMessageTypeWithSourceType_whenDeserializedAs_isForwardCompatible() {
        Contract.specification()
                .given(Snapshot.forMessageType("ShoppingCartConfirmed", ShoppingCartConfirmed.class))
                .whenDeserializedAs(ShoppingCartConfirmed.class)
                .thenForwardCompatible(event -> assertEquals(FIXED_CART_ID, event.shoppingCartId()));
    }

    @Test
    void given_snapshotByPath_whenDeserializedAs_isBackwardCompatibleWithExtraAssertion() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/ShoppingCartConfirmed.approved.txt");

        Contract.specification()
                .given(Snapshot.at(path, ShoppingCartConfirmed.class))
                .whenDeserializedAs(ShoppingCartConfirmed.class)
                .thenBackwardCompatible(event -> assertEquals(FIXED_CART_ID, event.shoppingCartId()));
    }

    @Test
    void given_snapshotThatDeserializesToNull_thenForwardCompatible_fails() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/Null.approved.txt");

        var error = assertThrows(
                AssertionError.class,
                () -> Contract.specification()
                        .given(Snapshot.at(path, ShoppingCartConfirmed.class))
                        .whenDeserializedAs(ShoppingCartConfirmed.class)
                        .thenForwardCompatible());

        assertTrue(requireNonNull(error.getMessage()).contains("returned empty"));
    }

    @Test
    void given_malformedSnapshot_whenDeserializedAs_wrapsDeserializationFailureInRuntimeException() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/Malformed.approved.txt");

        var exception = assertThrows(
                RuntimeException.class,
                () -> Contract.specification()
                        .given(Snapshot.at(path, ShoppingCartConfirmed.class))
                        .whenDeserializedAs(ShoppingCartConfirmed.class)
                        .thenForwardCompatible());

        assertTrue(requireNonNull(exception.getMessage()).contains("Deserialization as ShoppingCartConfirmed failed"));
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void given_missingSnapshotFile_whenDeserializedAs_wrapsFileReadFailureInRuntimeException() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/DoesNotExist.approved.txt");

        var exception = assertThrows(
                RuntimeException.class,
                () -> Contract.specification()
                        .given(Snapshot.at(path, ShoppingCartConfirmed.class))
                        .whenDeserializedAs(ShoppingCartConfirmed.class)
                        .thenForwardCompatible());

        assertTrue(requireNonNull(exception.getMessage()).contains("Cannot read snapshot file"));
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void given_unserializableInstance_whenDeserializedAs_wrapsSerializationFailureInRuntimeException() {
        var exception = assertThrows(
                RuntimeException.class,
                () -> Contract.specification()
                        .given(new Unserializable(FIXED_CART_ID))
                        .whenDeserializedAs(EmptyEvent.class)
                        .thenForwardCompatible());

        assertTrue(requireNonNull(exception.getMessage()).contains("Serialization of Unserializable failed"));
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void given_instanceThatDeserializesToUnserializableTarget_wrapsTargetSerializationFailure() {
        var exception = assertThrows(
                RuntimeException.class,
                () -> Contract.specification()
                        .given(new ShoppingCartConfirmed(FIXED_CART_ID, "anonymised", FIXED_DATE))
                        .whenDeserializedAs(Unserializable.class)
                        .thenForwardCompatible());

        assertTrue(exception.getCause() instanceof IOException);
    }
}
