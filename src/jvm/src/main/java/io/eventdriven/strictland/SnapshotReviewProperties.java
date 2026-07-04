package io.eventdriven.strictland;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.Nullable;

/**
 * Reads a {@link SnapshotReview} from properties, so a project fixes how drifts are reviewed once in
 * configuration rather than in every test. The same two keys work in a {@code strictland.properties}
 * file a project ships and as {@code -D} system properties on a single run - the latter is how you
 * re-baseline on demand with {@code -Dstrictland.review.mode=approve}.
 *
 * <p>Both keys are optional; when neither is present the result is empty, so the caller falls through
 * to the next level of configuration. The keys are {@code strictland.review.mode} ({@code auto},
 * {@code off}, or {@code approve}) and {@code strictland.review.tool} (a registered tool name such as
 * {@code meld}, or a full {@code path {received} {approved}} template).</p>
 *
 * <pre>
 * var props = new Properties();
 * props.setProperty("strictland.review.mode", "approve");
 * SnapshotReview review = SnapshotReviewProperties.fromProperties(props).orElseThrow();
 * </pre>
 */
public final class SnapshotReviewProperties {

    private static final String DEFAULT_RESOURCE = "strictland.properties";
    private static final String MODE_KEY = "strictland.review.mode";
    private static final String TOOL_KEY = "strictland.review.tool";

    private SnapshotReviewProperties() {}

    /**
     * Reads {@code strictland.properties} from the classpath, returning the review it describes, or
     * empty when the file is absent or sets no review keys.
     *
     * @return the review read from {@code strictland.properties}, or empty when none is configured
     */
    public static Optional<SnapshotReview> fromClasspath() {
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

    /**
     * Converts properties into a {@link SnapshotReview}, or empty when neither review key is present so
     * the caller can fall through to the next configuration level. This is the pure core the classpath
     * and system-property readers share.
     *
     * <pre>
     * var props = new Properties();
     * props.setProperty("strictland.review.mode", "off");
     * SnapshotReview review = SnapshotReviewProperties.fromProperties(props).orElseThrow();
     * </pre>
     *
     * @param props the configuration to read the review from
     * @return the review the properties describe, or empty when they set no review keys
     * @throws IllegalArgumentException when the mode or tool value isn't recognised
     */
    public static Optional<SnapshotReview> fromProperties(Properties props) {
        var mode = props.getProperty(MODE_KEY);
        var tool = props.getProperty(TOOL_KEY);
        if (mode == null && tool == null) {
            return Optional.empty();
        }
        var review = SnapshotReview.auto();
        if (mode != null) {
            review = review.withMode(parseMode(mode));
        }
        if (tool != null && !tool.isBlank()) {
            var resolved = DiffTools.fromSetting(tool)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown diff tool: " + tool));
            review = review.withTool(resolved);
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
