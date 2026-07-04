package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * How a snapshot check reviews a drift: whether it opens a local diff tool, stays quiet, or promotes
 * the received payload over the approved snapshot. It falls through the same config chain as {@link
 * SnapshotLayout} - a per-spec value, then the global {@link Strictland} default, then {@code
 * strictland.properties}, then the built-in {@link #auto()}.
 *
 * <p>Start from a factory that reads as the intent: {@link #auto()} to diff and, on a local machine,
 * open a tool; {@link #off()} to keep the inline diff but never open a tool; {@link #approve()} to
 * re-baseline a change on purpose. Pick a single diff tool by registered name with {@link
 * #tool(String)}, or give auto mode a preferred order with {@link #toolOrder(String...)}.</p>
 *
 * <pre>
 * SpecificationOptions options = Json.Jackson.defaults().snapshotReview(SnapshotReview.tool("meld"));
 * </pre>
 */
public final class SnapshotReview {

    private final ReviewMode mode;
    private final @Nullable ToolPreference toolPreference;

    private SnapshotReview(ReviewMode mode, @Nullable ToolPreference toolPreference) {
        this.mode = requireNonNull(mode, "mode");
        this.toolPreference = toolPreference;
    }

    /**
     * The default review: render the inline diff, and on a local, interactive machine open a detected
     * diff tool. On CI or a headless machine the inline diff stands alone.
     *
     * @return the default automatic review
     */
    public static SnapshotReview auto() {
        return new SnapshotReview(ReviewMode.AUTO, null);
    }

    /**
     * A review that keeps the inline diff in the failure message but never opens a diff tool, for when
     * a tool would get in the way.
     *
     * @return a review that never launches a diff tool
     */
    public static SnapshotReview off() {
        return new SnapshotReview(ReviewMode.OFF, null);
    }

    /**
     * A review that promotes the received payload over the approved snapshot on drift instead of
     * failing, for re-baselining a message you changed on purpose.
     *
     * @return a review that approves drifted snapshots
     */
    public static SnapshotReview approve() {
        return new SnapshotReview(ReviewMode.APPROVE, null);
    }

    /**
     * A review that launches the named diff tool rather than an auto-selected one. If that tool is not
     * available at launch time, Strictland keeps the inline diff and does not silently choose another
     * GUI tool. The name is one Strictland knows, such as {@code "vscode"}, {@code "idea"}, or {@code
     * "meld"}.
     *
     * <pre>
     * SnapshotReview review = SnapshotReview.tool("meld");
     * </pre>
     *
     * @param name the logical name of a registered diff tool
     * @return an automatic review that launches the named tool when it is available
     * @throws IllegalArgumentException when no registered tool has that name
     */
    public static SnapshotReview tool(String name) {
        return new SnapshotReview(ReviewMode.AUTO, ToolPreference.single(DiffTools.requireName(name)));
    }

    /**
     * A review that gives auto mode a preferred registered tool order. Listed tools are tried first in
     * the order provided, then the remaining built-in tools, with Git's no-index difftool fallback last.
     *
     * <pre>
     * SnapshotReview review = SnapshotReview.toolOrder("idea", "vscode", "meld");
     * </pre>
     *
     * @param names registered tool names in preferred order
     * @return an automatic review with the supplied tool preference order
     * @throws IllegalArgumentException when the order is empty or contains an unknown tool name
     */
    public static SnapshotReview toolOrder(String... names) {
        var ordered = Arrays.stream(names).map(DiffTools::requireName).toList();
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("Tool order must name at least one registered diff tool.");
        }
        return new SnapshotReview(ReviewMode.AUTO, ToolPreference.order(ordered));
    }

    ReviewMode mode() {
        return mode;
    }

    @Nullable ToolPreference toolPreference() {
        return toolPreference;
    }

    SnapshotReview withMode(ReviewMode mode) {
        return new SnapshotReview(mode, toolPreference);
    }

    SnapshotReview withToolPreference(@Nullable ToolPreference toolPreference) {
        return new SnapshotReview(mode, toolPreference);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof SnapshotReview other
                && mode == other.mode
                && Objects.equals(toolPreference, other.toolPreference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, toolPreference);
    }

    @Override
    public String toString() {
        return "SnapshotReview[mode=" + mode + ", toolPreference=" + toolPreference + "]";
    }
}
