package io.eventdriven.strictland;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The process-wide defaults a specification falls back to when it doesn't set a value itself, so you
 * choose a snapshot layout once for your whole suite rather than on every test. Reach for {@link
 * #defaults()} in a test setup or a base class to set them, and {@link #resetDefaults()} in teardown
 * to leave no trace for the next test.
 *
 * <p>These sit between a per-spec {@link SpecificationOptions} value and what {@code
 * strictland.properties} ships. A spec that sets nothing takes the global default; a spec that sets a
 * value overrides it for that setting alone. Each setting is independent: setting one leaves the
 * others unset.</p>
 *
 * <pre>
 * Strictland.defaults().snapshotLayout(SnapshotLayout.registry());
 * try {
 *     MessageContract.specification(Json.Jackson.defaults())
 *         .given(new OrderPlaced(orderId, "Alice", placedAt))
 *         .whenSerialized()
 *         .thenContractIsUnchanged();
 * } finally {
 *     Strictland.resetDefaults();
 * }
 * </pre>
 */
public final class Strictland {

    private static volatile @Nullable SnapshotLayout snapshotLayout;

    private Strictland() {}

    /**
     * Returns the mutable global configuration, where you set the defaults a specification falls back
     * to. Each setter sets one value and returns the same configuration, so you can chain calls. A
     * value you never set stays unset, and a spec that needs it falls through to the file level and
     * then the built-in default.
     *
     * <pre>
     * Strictland.defaults().snapshotLayout(SnapshotLayout.registry().rootPath("src/test/resources"));
     * </pre>
     *
     * @return the global configuration to set defaults on
     */
    public static Config defaults() {
        return new Config();
    }

    /**
     * Clears every global default, restoring the built-in behaviour. Call this in test teardown so a
     * default one test sets can't leak into the next, which would make the suite order-dependent.
     *
     * <pre>
     * Strictland.defaults().snapshotLayout(SnapshotLayout.registry());
     * try {
     *     // a check that relies on the global default
     * } finally {
     *     Strictland.resetDefaults();
     * }
     * </pre>
     */
    public static void resetDefaults() {
        snapshotLayout = null;
    }

    static Optional<SnapshotLayout> snapshotLayout() {
        return Optional.ofNullable(snapshotLayout);
    }

    /**
     * The mutable global configuration returned by {@link Strictland#defaults()}. Each setter records
     * one default and returns this same instance so calls chain, and each setting is independent: one
     * you don't set stays unset.
     *
     * <pre>
     * Strictland.defaults().snapshotLayout(SnapshotLayout.registry());
     * </pre>
     */
    public static final class Config {

        private Config() {}

        /**
         * Sets the global default snapshot layout, used by any specification that doesn't set its own.
         * Pair it with {@link Strictland#resetDefaults()} in teardown so it doesn't outlive the test.
         *
         * <pre>
         * Strictland.defaults().snapshotLayout(SnapshotLayout.registry().wrapperFolder("approved"));
         * </pre>
         *
         * @param layout the layout to fall back to when a spec sets none
         * @return this configuration, so you can chain further calls
         */
        public Config snapshotLayout(SnapshotLayout layout) {
            Strictland.snapshotLayout = layout;
            return this;
        }
    }
}
