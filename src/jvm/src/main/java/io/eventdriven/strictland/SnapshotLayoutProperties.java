package io.eventdriven.strictland;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.Nullable;

/**
 * Reads a {@link SnapshotLayout} from a {@code strictland.properties} file, so a project can pick its
 * snapshot layout once in configuration rather than in every test. Reach for {@link
 * #fromClasspath()} to honour a file a project ships, and {@link #fromProperties(Properties)} when
 * you already hold the values and want a pure, testable conversion.
 *
 * <p>Every key is optional and falls back to the {@link SnapshotLayout#registry()} defaults. The
 * recognised keys are {@code strictland.layout.rootPath}, {@code strictland.layout.wrapperFolder}, and
 * {@code strictland.layout.testClassNaming} ({@code simple} or {@code full}). An unrecognised
 * {@code testClassNaming} value throws {@link IllegalArgumentException} naming the key and the
 * value.</p>
 *
 * <pre>
 * var props = new Properties();
 * props.setProperty("strictland.layout.rootPath", "src/test/resources");
 * props.setProperty("strictland.layout.wrapperFolder", "contract-snapshots");
 * SnapshotLayout layout = SnapshotLayoutProperties.fromProperties(props);
 * </pre>
 */
public final class SnapshotLayoutProperties {

    private static final String DEFAULT_RESOURCE = "strictland.properties";
    private static final String ROOT_PATH_KEY = "strictland.layout.rootPath";
    private static final String WRAPPER_FOLDER_KEY = "strictland.layout.wrapperFolder";
    private static final String TEST_CLASS_NAMING_KEY = "strictland.layout.testClassNaming";

    private SnapshotLayoutProperties() {}

    /**
     * Reads {@code strictland.properties} from the classpath, returning the layout it describes, or
     * empty when no such file is present. Use this to let a project opt into a layout by shipping the
     * file, while a project that doesn't keeps the {@link SnapshotLayout#registry()} defaults.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayoutProperties.fromClasspath().orElseGet(SnapshotLayout::registry);
     * </pre>
     *
     * @return the layout read from {@code strictland.properties}, or empty if the file is absent
     */
    public static Optional<SnapshotLayout> fromClasspath() {
        return fromClasspath(DEFAULT_RESOURCE);
    }

    static Optional<SnapshotLayout> fromClasspath(String resourceName) {
        return fromStream(
                resourceName, SnapshotLayoutProperties.class.getClassLoader().getResourceAsStream(resourceName));
    }

    static Optional<SnapshotLayout> fromStream(String source, @Nullable InputStream stream) {
        if (stream == null) {
            return Optional.empty();
        }
        try (stream) {
            var props = new Properties();
            props.load(stream);
            return Optional.of(fromProperties(props));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + source, e);
        }
    }

    /**
     * Converts properties into a {@link SnapshotLayout}, with any absent key falling back to the
     * {@link SnapshotLayout#registry()} default. This is the pure core the classpath reader builds
     * on, handy when you hold the values yourself, for instance in a test.
     *
     * <pre>
     * var props = new Properties();
     * props.setProperty("strictland.layout.wrapperFolder", "approved");
     * SnapshotLayout layout = SnapshotLayoutProperties.fromProperties(props);
     * </pre>
     *
     * @param props the configuration to read the layout from
     * @return the layout the properties describe
     * @throws IllegalArgumentException if a test-class-naming value is not recognised
     */
    public static SnapshotLayout fromProperties(Properties props) {
        var layout = SnapshotLayout.registry();

        var rootPath = props.getProperty(ROOT_PATH_KEY);
        if (rootPath != null) {
            layout = layout.rootPath(rootPath);
        }

        var wrapperFolder = props.getProperty(WRAPPER_FOLDER_KEY);
        if (wrapperFolder != null) {
            layout = layout.wrapperFolder(wrapperFolder);
        }

        var testClassNaming = props.getProperty(TEST_CLASS_NAMING_KEY);
        if (testClassNaming != null) {
            layout = layout.testClassNaming(testClassNaming(testClassNaming));
        }

        return layout;
    }

    private static TestClassNaming testClassNaming(String testClassNaming) {
        return switch (testClassNaming) {
            case "simple" -> TestClassNaming.SIMPLE;
            case "full" -> TestClassNaming.FULL;
            default -> throw badValue(TEST_CLASS_NAMING_KEY, testClassNaming);
        };
    }

    private static IllegalArgumentException badValue(String key, String value) {
        return new IllegalArgumentException("Unrecognised value '" + value + "' for property '" + key + "'");
    }
}
