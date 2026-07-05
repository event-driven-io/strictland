package io.eventdriven.strictland;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.Nullable;

final class SnapshotReviewProperties {

    static final String MODE_KEY = "strictland.review.mode";
    static final String TOOL_KEY = "strictland.review.tool";

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
        var mode = props.getProperty(MODE_KEY);
        var tool = props.getProperty(TOOL_KEY);
        if (mode == null && (tool == null || tool.isBlank())) {
            return Optional.empty();
        }
        var review = SnapshotReview.auto();
        if (mode != null) {
            review = review.withMode(parseMode(mode));
        }
        if (tool != null && !tool.isBlank()) {
            review = review.withToolPreference(DiffTools.fromToolSetting(tool));
        }
        return Optional.of(review);
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
