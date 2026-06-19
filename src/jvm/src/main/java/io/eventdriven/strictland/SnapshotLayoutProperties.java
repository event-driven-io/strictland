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
 * <p>Every key is optional and falls back to the {@link SnapshotLayout#nextToTest()} defaults. The
 * recognised keys are {@code strictland.layout.location} ({@code nextToTest} or {@code globalRoot}),
 * {@code strictland.layout.grouping} ({@code perTestClass}, {@code perContract}, or {@code none}),
 * {@code strictland.layout.wrapperFolder}, {@code strictland.layout.rootPath} (read only when the
 * location is {@code globalRoot}), and {@code strictland.layout.testClassNaming} ({@code simple} or
 * {@code full}). An unrecognised location, grouping, or test-class-naming value throws {@link
 * IllegalArgumentException} naming the key and the value.</p>
 *
 * <pre>
 * var props = new Properties();
 * props.setProperty("strictland.layout.location", "globalRoot");
 * props.setProperty("strictland.layout.rootPath", "src/test/resources/snapshots");
 * SnapshotLayout layout = SnapshotLayoutProperties.fromProperties(props);
 * </pre>
 */
public final class SnapshotLayoutProperties {

    private static final String DEFAULT_RESOURCE = "strictland.properties";
    private static final String LOCATION_KEY = "strictland.layout.location";
    private static final String GROUPING_KEY = "strictland.layout.grouping";
    private static final String WRAPPER_FOLDER_KEY = "strictland.layout.wrapperFolder";
    private static final String ROOT_PATH_KEY = "strictland.layout.rootPath";
    private static final String TEST_CLASS_NAMING_KEY = "strictland.layout.testClassNaming";

    private SnapshotLayoutProperties() {}

    /**
     * Reads {@code strictland.properties} from the classpath, returning the layout it describes, or
     * empty when no such file is present. Use this to let a project opt into a layout by shipping the
     * file, while a project that doesn't keeps the {@link SnapshotLayout#nextToTest()} defaults.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayoutProperties.fromClasspath().orElseGet(SnapshotLayout::nextToTest);
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
     * {@link SnapshotLayout#nextToTest()} default. This is the pure core the classpath reader builds
     * on, handy when you hold the values yourself, for instance in a test.
     *
     * <pre>
     * var props = new Properties();
     * props.setProperty("strictland.layout.grouping", "perContract");
     * SnapshotLayout layout = SnapshotLayoutProperties.fromProperties(props);
     * </pre>
     *
     * @param props the configuration to read the layout from
     * @return the layout the properties describe
     * @throws IllegalArgumentException if a location or grouping value is not recognised
     */
    public static SnapshotLayout fromProperties(Properties props) {
        var layout = startingLayout(props.getProperty(LOCATION_KEY), props.getProperty(ROOT_PATH_KEY));

        var grouping = props.getProperty(GROUPING_KEY);
        if (grouping != null) {
            layout = layout.grouping(grouping(grouping));
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

    private static SnapshotLayout startingLayout(@Nullable String location, @Nullable String rootPath) {
        if (location == null) {
            return SnapshotLayout.nextToTest();
        }
        return switch (location) {
            case "nextToTest" -> SnapshotLayout.nextToTest();
            case "globalRoot" -> SnapshotLayout.globalRoot(rootPath != null ? rootPath : "");
            default -> throw badValue(LOCATION_KEY, location);
        };
    }

    private static SnapshotGrouping grouping(String grouping) {
        return switch (grouping) {
            case "perTestClass" -> SnapshotGrouping.PER_TEST_CLASS;
            case "perContract" -> SnapshotGrouping.PER_CONTRACT;
            case "none" -> SnapshotGrouping.NONE;
            default -> throw badValue(GROUPING_KEY, grouping);
        };
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
