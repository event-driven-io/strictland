package io.eventdriven.strictland;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class SimpleBinaryMessageSerializer implements MessageSerializer {
    private static final int MAGIC = 0xCAFEBABE;

    @Override
    public byte[] serialize(Object value) {
        var components = value.getClass().getRecordComponents();
        var out = new ByteArrayOutputStream();
        try (var data = new DataOutputStream(out)) {
            data.writeInt(MAGIC);
            data.writeInt(components.length);
            for (var component : components) {
                data.writeUTF(readComponent(component, value).toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        var components = type.getRecordComponents();
        try (var data = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (data.readInt() != MAGIC) {
                throw new IllegalArgumentException("Not a binary snapshot: bad magic header");
            }
            var count = data.readInt();
            if (count != components.length) {
                throw new IllegalArgumentException(
                        "Component count mismatch: expected " + components.length + " but got " + count);
            }
            var values = new Object[count];
            var parameterTypes = new Class<?>[count];
            for (var i = 0; i < count; i++) {
                var component = components[i];
                var raw = data.readUTF();
                parameterTypes[i] = component.getType();
                values[i] = component.getType() == UUID.class ? UUID.fromString(raw) : raw;
            }
            var constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return type.cast(constructor.newInstance(values));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readComponent(RecordComponent component, Object value) {
        try {
            var accessor = component.getAccessor();
            accessor.setAccessible(true);
            var result = accessor.invoke(value);
            if (result == null) {
                throw new IllegalArgumentException("Null component not supported: " + component.getName());
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
