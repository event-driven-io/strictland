package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

final class StructuralComparison {
    private record Ancestor(Object source, Object target) {}

    private record MessageField(boolean nullable, @Nullable Object value) {}

    private interface FieldReader {
        @Nullable Object read() throws ReflectiveOperationException;
    }

    private final List<Ancestor> ancestors = new ArrayList<>();

    private StructuralComparison() {}

    static @Nullable String difference(@Nullable Object source, @Nullable Object target) {
        return new StructuralComparison().compare(source, target, "");
    }

    static @Nullable String missingRequiredField(Object target) {
        return difference(target, target);
    }

    private @Nullable String compare(@Nullable Object source, @Nullable Object target, String path) {
        var sourcePresent = presentValue(source);
        var targetPresent = presentValue(target);
        if (sourcePresent == null || targetPresent == null) {
            return sourcePresent == targetPresent ? null : valueDiffers(path, source, target);
        }
        if (sourcePresent instanceof Number sourceNumber && targetPresent instanceof Number targetNumber) {
            return sameNumber(sourceNumber, targetNumber) ? null : valueDiffers(path, source, target);
        }
        var sourceValue = enumAsName(sourcePresent);
        var targetValue = enumAsName(targetPresent);
        if (isContainer(sourceValue) || isContainer(targetValue)) {
            return nested(sourceValue, targetValue, () -> compareContainers(sourceValue, targetValue, path));
        }
        if (isApplicationType(sourceValue)
                && isApplicationType(targetValue)
                && !sharesCustomEquality(sourceValue, targetValue)) {
            return nested(sourceValue, targetValue, () -> compareObjects(sourceValue, targetValue, path));
        }
        return sourceValue.equals(targetValue) ? null : valueDiffers(path, source, target);
    }

    private @Nullable String nested(Object source, Object target, Supplier<@Nullable String> comparison) {
        for (var ancestor : ancestors) {
            if (ancestor.source() == source && ancestor.target() == target) return null;
        }
        ancestors.add(new Ancestor(source, target));
        try {
            return comparison.get();
        } finally {
            ancestors.remove(ancestors.size() - 1);
        }
    }

    private @Nullable String compareContainers(Object source, Object target, String path) {
        if (source instanceof Map<?, ?> sourceMap && target instanceof Map<?, ?> targetMap) {
            return compareMaps(sourceMap, targetMap, path);
        }
        if (isSequence(source) && isSequence(target)) {
            return compareSequences(source, target, path);
        }
        return valueDiffers(path, source, target);
    }

    private @Nullable String compareMaps(Map<?, ?> source, Map<?, ?> target, String path) {
        if (source.size() != target.size()) {
            return valueDiffers(path, "expected " + source.size() + " entries but found " + target.size());
        }
        var unmatched = new ArrayList<Map.Entry<?, ?>>(target.entrySet());
        for (var entry : source.entrySet()) {
            var key = entry.getKey();
            var match = unmatched.stream()
                    .filter(candidate -> compare(key, candidate.getKey(), path) == null)
                    .findFirst();
            if (match.isEmpty()) {
                return valueDiffers(path, "missing key <" + enumAsName(key) + ">");
            }
            unmatched.remove(match.get());
            var failure = compare(entry.getValue(), match.get().getValue(), path + "[" + enumAsName(key) + "]");
            if (failure != null) return failure;
        }
        return null;
    }

    private @Nullable String compareSequences(Object source, Object target, String path) {
        var sourceItems = itemsOf(source);
        var targetItems = itemsOf(target);
        if (sourceItems.size() != targetItems.size()) {
            return valueDiffers(path, "expected " + sourceItems.size() + " items but found " + targetItems.size());
        }
        if (isOrdered(source) && isOrdered(target)) {
            for (var i = 0; i < sourceItems.size(); i++) {
                var failure = compare(sourceItems.get(i), targetItems.get(i), path + "[" + i + "]");
                if (failure != null) return failure;
            }
            return null;
        }
        var used = new boolean[targetItems.size()];
        for (var sourceItem : sourceItems) {
            var match = -1;
            for (var i = 0; i < targetItems.size() && match < 0; i++) {
                if (!used[i] && compare(sourceItem, targetItems.get(i), path) == null) match = i;
            }
            if (match < 0) {
                return valueDiffers(path, "no matching item for <" + sourceItem + ">");
            }
            used[match] = true;
        }
        return null;
    }

    private @Nullable String compareObjects(Object source, Object target, String path) {
        var sourceFields = fieldsOf(source);
        var targetFields = fieldsOf(target);

        for (var entry : targetFields.entrySet()) {
            var targetField = entry.getValue();
            if (targetField.nullable()) continue;
            var sourceField = sourceFields.get(entry.getKey());
            if (targetField.value() == null || sourceField == null || sourceField.value() == null) {
                return "Required field '"
                        + child(path, entry.getKey())
                        + "' in "
                        + target.getClass().getSimpleName()
                        + " is missing or null after deserialization";
            }
        }

        var sourceOnly = new ArrayList<>(sourceFields.keySet());
        sourceOnly.removeAll(targetFields.keySet());
        var targetOnly = new ArrayList<>(targetFields.keySet());
        targetOnly.removeAll(sourceFields.keySet());
        if (!sourceOnly.isEmpty() && !targetOnly.isEmpty()) {
            return "Structural incompatibility detected"
                    + (path.isEmpty() ? "" : " at '" + path + "'")
                    + ": source and target have different unmapped fields [source-only: "
                    + sourceOnly
                    + ", target-only: "
                    + targetOnly
                    + "]";
        }

        for (var entry : sourceFields.entrySet()) {
            var targetField = targetFields.get(entry.getKey());
            if (targetField == null) continue;
            var failure = compare(entry.getValue().value(), targetField.value(), child(path, entry.getKey()));
            if (failure != null) return failure;
        }
        return null;
    }

    private static Map<String, MessageField> fieldsOf(Object message) {
        var fields = new LinkedHashMap<String, MessageField>();
        var type = message.getClass();
        if (type.isRecord()) {
            for (var component : type.getRecordComponents()) {
                var accessor = component.getAccessor();
                accessor.setAccessible(true);
                fields.put(
                        component.getName(),
                        field(
                                message,
                                component.getName(),
                                component.getAnnotatedType(),
                                () -> accessor.invoke(message)));
            }
            return fields;
        }
        for (Class<?> current = type; !isJdkType(current); current = requireNonNull(current.getSuperclass())) {
            for (var declared : current.getDeclaredFields()) {
                if (Modifier.isStatic(declared.getModifiers())) continue;
                declared.setAccessible(true);
                fields.put(
                        declared.getName(),
                        field(message, declared.getName(), declared.getAnnotatedType(), () -> declared.get(message)));
            }
        }
        return fields;
    }

    private static MessageField field(Object message, String name, AnnotatedType type, FieldReader reader) {
        try {
            return new MessageField(isOptional(type), reader.read());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Cannot read field '" + name + "' of " + message.getClass().getSimpleName(), e);
        }
    }

    private static boolean isOptional(AnnotatedType type) {
        return type.getDeclaredAnnotation(Nullable.class) != null
                || (type.getType() instanceof ParameterizedType parameterized
                        && parameterized.getRawType() == Optional.class);
    }

    private static @Nullable Object presentValue(@Nullable Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

    private static boolean sameNumber(Number source, Number target) {
        var sourceDecimal = decimalOf(source);
        var targetDecimal = decimalOf(target);
        if (sourceDecimal == null || targetDecimal == null) {
            return Double.compare(source.doubleValue(), target.doubleValue()) == 0;
        }
        return sourceDecimal.compareTo(targetDecimal) == 0;
    }

    private static @Nullable BigDecimal decimalOf(Number number) {
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Object enumAsName(Object value) {
        return value instanceof Enum<?> constant ? constant.name() : value;
    }

    private static boolean isContainer(Object value) {
        return value instanceof Map<?, ?> || isSequence(value);
    }

    private static boolean isSequence(Object value) {
        return value instanceof Collection<?> || value.getClass().isArray();
    }

    private static boolean isOrdered(Object sequence) {
        return sequence instanceof List<?> || sequence.getClass().isArray();
    }

    private static List<@Nullable Object> itemsOf(Object sequence) {
        if (sequence instanceof Collection<?> items) return new ArrayList<>(items);
        var items = new ArrayList<@Nullable Object>();
        for (var i = 0; i < Array.getLength(sequence); i++) {
            items.add(Array.get(sequence, i));
        }
        return items;
    }

    private static boolean isApplicationType(Object value) {
        return !isJdkType(value.getClass());
    }

    private static boolean isJdkType(Class<?> type) {
        var loader = type.getClassLoader();
        return loader == null || loader == ClassLoader.getPlatformClassLoader();
    }

    private static boolean sharesCustomEquality(Object source, Object target) {
        var type = source.getClass();
        return type == target.getClass()
                && !type.isRecord()
                && Arrays.stream(type.getMethods())
                        .anyMatch(method ->
                                method.getName().equals("equals") && method.getDeclaringClass() != Object.class);
    }

    private static String child(String path, String name) {
        return path.isEmpty() ? name : path + "." + name;
    }

    private static String valueDiffers(String path, @Nullable Object source, @Nullable Object target) {
        return valueDiffers(path, "source: <" + source + "> but target: <" + target + ">");
    }

    private static String valueDiffers(String path, String detail) {
        return "Field '" + (path.isEmpty() ? "$" : path) + "' value differs between versions ==> " + detail;
    }
}
