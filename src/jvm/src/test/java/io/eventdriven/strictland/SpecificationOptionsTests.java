package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SpecificationOptionsTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private record MemberJoined(UUID memberId, String name) {}

    private record CustomMessageSerializer(JsonMapper mapper) implements MessageSerializer {
        @Override
        public byte[] serialize(Object value) {
            try {
                return mapper.writeValueAsBytes(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public <T> T deserialize(byte[] bytes, Class<T> type) {
            try {
                return mapper.readValue(bytes, type);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    void givenWithers_whenApplied_thenAccessorsReturnWhatWasSet() {
        MessageSerializer serializer = new CustomMessageSerializer(new JsonMapper());
        SnapshotStorage storage = new FileSnapshotStorage();
        MessageTypeMapper typeMapper = MessageTypeMapper.simpleName();

        var options = SpecificationOptions.serializer(serializer)
                .snapshotStorage(storage)
                .messageTypeMapper(typeMapper);

        assertSame(serializer, options.serializer());
        assertSame(storage, options.storage());
        assertSame(typeMapper, options.typeMapper());
    }

    @Test
    void givenSimpleNameMapper_whenQueried_thenItNamesByClassAndResolvesNoName() {
        var mapper = MessageTypeMapper.simpleName();

        assertEquals("String", mapper.name(String.class));
        assertTrue(mapper.type("anything").isEmpty());
    }

    @Test
    void givenANonJacksonSerializer_whenCheckedForCompatibility_thenTier1Runs() {
        var captured = new boolean[1];

        MessageContract.specification(SpecificationOptions.serializer(new CustomMessageSerializer(new JsonMapper())))
                .given(new MemberJoined(FIXED_ID, "Alice"))
                .whenDeserializedAs(MemberJoined.class)
                .thenBackwardCompatible(member -> {
                    captured[0] = true;
                    assertEquals("Alice", member.name());
                });

        assertTrue(captured[0]);
    }

    @Test
    void givenANonJacksonSerializer_whenCheckedForForwardCompatibility_thenTier1RunsWithoutFieldDiff() {
        MessageContract.specification(SpecificationOptions.serializer(new CustomMessageSerializer(new JsonMapper())))
                .given(new MemberJoined(FIXED_ID, "Alice"))
                .whenDeserializedAs(MemberJoined.class)
                .thenForwardCompatible();
    }
}
