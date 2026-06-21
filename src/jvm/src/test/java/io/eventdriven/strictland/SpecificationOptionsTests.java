package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        MessageTypeMapper typeMapper = MessageTypeMapper.fullyQualifiedName();
        SnapshotLayout layout = SnapshotLayout.registry();

        var options = SpecificationOptions.serializer(serializer)
                .snapshotStorage(storage)
                .messageTypeMapper(typeMapper)
                .snapshotLayout(layout);

        assertSame(serializer, options.serializer());
        assertSame(storage, options.storage());
        assertSame(typeMapper, options.typeMapper());
        assertSame(layout, options.snapshotLayout());
    }

    @Test
    void givenSerializerOnly_whenCreated_thenEveryOtherSettingIsUnset() {
        var options = SpecificationOptions.serializer(new CustomMessageSerializer(new JsonMapper()));

        assertNull(options.storage());
        assertNull(options.typeMapper());
        assertNull(options.snapshotLayout());
    }

    @Test
    void givenACustomTypeMapper_whenContractBuilt_thenItNamesTheStoredSnapshot() {
        var captured = new String[1];
        SnapshotStorage storage = new SnapshotStorage() {
            @Override
            public void store(String name, byte[] payload) {
                captured[0] = name;
            }

            @Override
            public java.util.Optional<byte[]> read(String name) {
                return java.util.Optional.empty();
            }
        };

        MessageContract.specification(SpecificationOptions.serializer(new CustomMessageSerializer(new JsonMapper()))
                        .snapshotStorage(storage)
                        .messageTypeMapper(MessageTypeMapper.fullyQualifiedName()))
                .given(new MemberJoined(FIXED_ID, "Alice"))
                .whenSerialized()
                .thenContractIsUnchanged();

        assertEquals(
                "MemberJoined.1.SpecificationOptionsTests.givenACustomTypeMapper_whenContractBuilt_thenItNamesTheStoredSnapshot",
                captured[0]);
    }

    @Test
    void filesPreset_buildsFileBackedStorage() {
        assertTrue(Snapshots.files() instanceof FileSnapshotStorage);
    }

    @Test
    void givenFullyQualifiedNameMapper_whenQueried_thenItNamesByClassAndResolvesNoName() {
        var mapper = MessageTypeMapper.fullyQualifiedName();

        assertEquals("java.lang.String", mapper.name(String.class));
        assertTrue(mapper.type("anything").isEmpty());
    }

    @Test
    void givenFullyQualifiedNameMapper_whenNamingALocalClassWithNoCanonicalName_thenItFallsBackToTheBinaryName() {
        record LocalEvent() {}
        var mapper = MessageTypeMapper.fullyQualifiedName();

        assertNull(LocalEvent.class.getCanonicalName());
        assertEquals(LocalEvent.class.getTypeName(), mapper.name(LocalEvent.class));
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
