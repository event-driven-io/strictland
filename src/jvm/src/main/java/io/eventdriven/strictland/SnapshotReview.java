package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * How a snapshot check reviews a drift: which {@link ReviewMode} it runs in, and, optionally, an
 * explicit {@link DiffTool} to launch instead of the auto-detected one. It falls through the same
 * config chain as {@link SnapshotLayout} - a per-spec value, then the global {@link Strictland}
 * default, then {@code strictland.properties}, then the built-in {@link #auto()}.
 *
 * <p>Start from a factory that reads as the intent: {@link #auto()} to diff and, on a local machine,
 * open a tool; {@link #off()} to keep the inline diff but never open a tool; {@link #approve()} to
 * re-baseline a change on purpose. Pick the diff tool by name with {@link #tool(String)} when
 * auto-detection wouldn't choose the one you want.</p>
 *
 * <pre>
 * SpecificationOptions options = Json.Jackson.defaults().snapshotReview(SnapshotReview.tool("meld"));
 * </pre>
 *
 * @param mode what the check does on drift
 * @param tool the diff tool to launch, or null to auto-detect one
 */
public record SnapshotReview(ReviewMode mode, @Nullable DiffTool tool) {

    /**
     * The default review: render the inline diff, and on a local, interactive machine open a detected
     * diff tool. On CI or a headless machine the inline diff stands alone.
     *
     * @return a review in {@link ReviewMode#AUTO}
     */
    public static SnapshotReview auto() {
        return new SnapshotReview(ReviewMode.AUTO, null);
    }

    /**
     * A review that keeps the inline diff in the failure message but never opens a diff tool, for when
     * a tool would get in the way.
     *
     * @return a review in {@link ReviewMode#OFF}
     */
    public static SnapshotReview off() {
        return new SnapshotReview(ReviewMode.OFF, null);
    }

    /**
     * A review that promotes the received payload over the approved snapshot on drift instead of
     * failing, for re-baselining a message you changed on purpose.
     *
     * @return a review in {@link ReviewMode#APPROVE}
     */
    public static SnapshotReview approve() {
        return new SnapshotReview(ReviewMode.APPROVE, null);
    }

    /**
     * A review that launches the named diff tool rather than an auto-detected one, in {@link
     * ReviewMode#AUTO}. The name is one Strictland knows, such as {@code "vscode"}, {@code "idea"}, or
     * {@code "meld"}.
     *
     * <pre>
     * SnapshotReview review = SnapshotReview.tool("meld");
     * </pre>
     *
     * @param name the logical name of a registered diff tool
     * @return a review in {@link ReviewMode#AUTO} that launches the named tool
     * @throws IllegalArgumentException when no registered tool has that name
     */
    public static SnapshotReview tool(String name) {
        var resolved = DiffTools.byName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown diff tool: " + name + ". Known tools: "
                        + DiffTools.registry().stream().map(DiffTool::name).toList()));
        return new SnapshotReview(ReviewMode.AUTO, resolved);
    }

    /**
     * A review that launches the given diff tool, in {@link ReviewMode#AUTO}, for a tool Strictland
     * doesn't know by name.
     *
     * @param tool the diff tool to launch
     * @return a review in {@link ReviewMode#AUTO} that launches the given tool
     */
    public static SnapshotReview tool(DiffTool tool) {
        return new SnapshotReview(ReviewMode.AUTO, tool);
    }

    /**
     * Returns a copy in the given mode, keeping any tool this review already carries.
     *
     * @param mode what the check should do on drift
     * @return a copy of this review in the given mode
     */
    public SnapshotReview withMode(ReviewMode mode) {
        return new SnapshotReview(mode, tool);
    }

    /**
     * Returns a copy that launches the given tool, keeping this review's mode.
     *
     * @param tool the diff tool to launch
     * @return a copy of this review carrying the given tool
     */
    public SnapshotReview withTool(DiffTool tool) {
        return new SnapshotReview(mode, tool);
    }
}
