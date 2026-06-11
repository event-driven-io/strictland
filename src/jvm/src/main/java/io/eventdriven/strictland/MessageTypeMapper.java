package io.eventdriven.strictland;

interface MessageTypeMapper {
    String name(Class<?> type);

    static MessageTypeMapper simpleName() {
        return Class::getSimpleName;
    }
}
