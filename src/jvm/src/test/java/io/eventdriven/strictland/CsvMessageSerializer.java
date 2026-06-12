package io.eventdriven.strictland;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class CsvMessageSerializer implements MessageSerializer {

    @Override
    public byte[] serialize(Object value) {
        var components = value.getClass().getRecordComponents();
        var tokens = new String[components.length];
        for (var i = 0; i < components.length; i++) {
            tokens[i] = String.valueOf(read(components[i], value));
        }
        return String.join(",", tokens).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        var components = type.getRecordComponents();
        var tokens = splitTokens(bytes);
        var arguments = constructorArguments(components, tokens);
        var constructor = canonicalConstructor(type, components);
        return instantiate(type, constructor, arguments);
    }

    private static String[] splitTokens(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).split(",", -1);
    }

    private static @Nullable Object[] constructorArguments(RecordComponent[] components, String[] tokens) {
        var arguments = new @Nullable Object[components.length];
        for (var i = 0; i < components.length; i++) {
            arguments[i] = i < tokens.length ? convert(components[i].getType(), tokens[i]) : null;
        }
        return arguments;
    }

    private static <T> Constructor<T> canonicalConstructor(Class<T> type, RecordComponent[] components) {
        var parameterTypes = new Class<?>[components.length];
        for (var i = 0; i < components.length; i++) {
            parameterTypes[i] = components[i].getType();
        }
        try {
            var constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("CSV deserialization as " + type.getSimpleName() + " failed", e);
        }
    }

    private static <T> T instantiate(Class<T> type, Constructor<T> constructor, @Nullable Object[] arguments) {
        try {
            return type.cast(constructor.newInstance(arguments));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("CSV deserialization as " + type.getSimpleName() + " failed", e);
        }
    }

    private static @Nullable Object read(RecordComponent component, Object value) {
        try {
            var accessor = component.getAccessor();
            accessor.setAccessible(true);
            return accessor.invoke(value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Reading CSV component " + component.getName() + " failed", e);
        }
    }

    private static Object convert(Class<?> type, String token) {
        return type == UUID.class ? UUID.fromString(token) : token;
    }
}
