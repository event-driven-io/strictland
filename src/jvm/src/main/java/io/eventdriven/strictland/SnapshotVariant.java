package io.eventdriven.strictland;

/**
 * A named snapshot variant for the specific set of test data
 */
public sealed interface SnapshotVariant permits SnapshotVariant.ByLabel {

    /**
     * A named snapshot variant (e.g. "WithOnlyRequiredData")
     *
     * @param name the label naming the variant of the snapshotted message
     */
    public record ByLabel(String name) implements SnapshotVariant {}

    /**
     * A named snapshot variant (e.g. "WithOnlyRequiredData")
     *
     * @param name the label naming the variant of the snapshotted message
     * @return snapshot variant with text name
     */
    public static ByLabel named(String name) {
        return new ByLabel(name);
    }
}
