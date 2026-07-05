package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

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
 * re-baseline a change on purpose. Pick a specific diff tool with {@link #tool(DiffTool)}, or a custom
 * command line for a tool Strictland does not know with {@link #customTool(String)}.</p>
 *
 * <pre>
 * SpecificationOptions options = Json.Jackson.defaults().snapshotReview(SnapshotReview.tool(DiffTool.MELD));
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
     * A review that launches a specific known diff tool rather than an auto-selected one. If that tool
     * is not available at launch time, Strictland keeps the inline diff and does not silently choose
     * another GUI tool.
     *
     * <pre>
     * SnapshotReview review = SnapshotReview.tool(DiffTool.MELD);
     * </pre>
     *
     * @param tool the diff tool to launch
     * @return an automatic review that launches the given tool when it is available
     */
    public static SnapshotReview tool(DiffTool tool) {
        return new SnapshotReview(ReviewMode.AUTO, new ToolPreference.Named(requireNonNull(tool, "tool")));
    }

    /**
     * A review that launches a custom diff command, for a tool Strictland does not list. The command's
     * first token is the executable; {@code {received}} and {@code {approved}} are replaced with the two
     * files. If the executable is not available at launch time, Strictland keeps the inline diff.
     *
     * <pre>
     * SnapshotReview review = SnapshotReview.customTool("my-diff --wait {received} {approved}");
     * </pre>
     *
     * @param command the command line to launch, with {@code {received}} and {@code {approved}} placeholders
     * @return an automatic review that launches the custom command when its executable is available
     * @throws IllegalArgumentException when the command is blank
     */
    public static SnapshotReview customTool(String command) {
        return new SnapshotReview(ReviewMode.AUTO, new ToolPreference.Custom(command));
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
