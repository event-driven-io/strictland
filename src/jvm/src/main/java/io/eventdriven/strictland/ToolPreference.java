package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.jspecify.annotations.Nullable;

record ToolPreference(
        Kind kind, List<String> names, @Nullable String template) {

    enum Kind {
        SINGLE,
        ORDER,
        CUSTOM
    }

    ToolPreference {
        requireNonNull(kind, "kind");
        names = List.copyOf(names);
        if (kind != Kind.CUSTOM && template != null) {
            throw new IllegalArgumentException("Only custom tool preferences carry a template.");
        }
        if (kind == Kind.CUSTOM) {
            requireNonNull(template, "template");
            if (template.isBlank()) {
                throw new IllegalArgumentException("Custom diff command must not be blank.");
            }
        }
        if (kind != Kind.CUSTOM && names.isEmpty()) {
            throw new IllegalArgumentException("Tool preference must name at least one tool.");
        }
    }

    static ToolPreference single(String name) {
        return new ToolPreference(Kind.SINGLE, List.of(name), null);
    }

    static ToolPreference order(List<String> names) {
        return new ToolPreference(Kind.ORDER, names, null);
    }

    static ToolPreference custom(String template) {
        return new ToolPreference(Kind.CUSTOM, List.of(), template);
    }
}
