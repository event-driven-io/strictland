package io.eventdriven.strictland;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.Nullable;

final class SnapshotReviewProperties {

    static final String MODE_KEY = "strictland.review.mode";
    static final String TOOL_KEY = "strictland.review.tool";
    static final String TOOL_ORDER_KEY = "strictland.review.toolOrder";
    static final String TOOL_ORDER_ENV = "STRICTLAND_REVIEW_TOOL_ORDER";

    private static final String DEFAULT_RESOURCE = "strictland.properties";

    private SnapshotReviewProperties() {}

    static Optional<SnapshotReview> fromClasspath() {
        return fromClasspath(DEFAULT_RESOURCE);
    }

    static Optional<SnapshotReview> fromClasspath(String resourceName) {
        return fromStream(
                resourceName, SnapshotReviewProperties.class.getClassLoader().getResourceAsStream(resourceName));
    }

    static Optional<SnapshotReview> fromStream(String source, @Nullable InputStream stream) {
        if (stream == null) {
            return Optional.empty();
        }
        try (stream) {
            var props = new Properties();
            props.load(stream);
            return fromProperties(props);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + source, e);
        }
    }

    static Optional<SnapshotReview> fromProperties(Properties props) {
        return fromPropertiesAndEnv(props, Map.of());
    }

    static Optional<SnapshotReview> fromPropertiesAndEnv(Properties props, Map<String, String> env) {
        var mode = props.getProperty(MODE_KEY);
        var tool = props.getProperty(TOOL_KEY);
        var order = props.getProperty(TOOL_ORDER_KEY);
        var envOrder = env.get(TOOL_ORDER_ENV);
        if (mode == null && tool == null && order == null && (envOrder == null || envOrder.isBlank())) {
            return Optional.empty();
        }
        var review = SnapshotReview.auto();
        if (mode != null) {
            review = review.withMode(parseMode(mode));
        }
        var preference = preference(tool, order, envOrder);
        if (preference != null) {
            review = review.withToolPreference(preference);
        }
        return Optional.of(review);
    }

    private static @Nullable ToolPreference preference(
            @Nullable String tool, @Nullable String order, @Nullable String envOrder) {
        if (tool != null && !tool.isBlank()) {
            return DiffTools.fromToolSetting(tool);
        }
        if (order != null && !order.isBlank()) {
            return ToolPreference.order(DiffTools.parseOrder(order));
        }
        if (envOrder == null) {
            return null;
        }
        if (envOrder.isBlank()) {
            return null;
        }
        return ToolPreference.order(DiffTools.parseOrder(envOrder));
    }

    private static ReviewMode parseMode(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> ReviewMode.AUTO;
            case "off" -> ReviewMode.OFF;
            case "approve" -> ReviewMode.APPROVE;
            default ->
                throw new IllegalArgumentException(
                        "Unknown review mode: " + value + ". Expected one of: auto, off, approve.");
        };
    }
}
