package io.eventdriven.strictland;

import java.util.ArrayList;
import java.util.List;

/**
 * A diff tool Strictland can launch on drift to show the received payload next to the approved
 * snapshot, so you review a change where you'd review any other: side by side, then save over the
 * approved file to accept it. You rarely build one yourself; {@link SnapshotReview#tool(String)} picks
 * a registered tool by name, and Strictland auto-detects one when you leave it unset. Build one
 * directly only for a tool Strictland doesn't know.
 *
 * <p>The {@code template} is the launch command with two placeholders, {@code {received}} and {@code
 * {approved}}, that {@link #command(String, String)} fills in with the two file paths. The convention,
 * shared with ApprovalTests and Verify, is received on the left and approved on the right.</p>
 *
 * <pre>
 * DiffTool meld = new DiffTool("meld", List.of("meld"), List.of("meld", "{received}", "{approved}"));
 * SnapshotReview review = SnapshotReview.tool(meld);
 * </pre>
 *
 * @param name the logical name you select the tool by, such as {@code "vscode"} or {@code "meld"}
 * @param candidates the executable names probed on the {@code PATH} to decide the tool is installed
 * @param template the launch command, holding {@code {received}} and {@code {approved}} placeholders
 */
public record DiffTool(String name, List<String> candidates, List<String> template) {

    /**
     * Holds the candidate and template lists as immutable copies, so a tool stays a stable value once
     * built.
     *
     * @param name the logical name you select the tool by
     * @param candidates the executable names probed on the {@code PATH}
     * @param template the launch command with {@code {received}}/{@code {approved}} placeholders
     */
    public DiffTool {
        candidates = List.copyOf(candidates);
        template = List.copyOf(template);
    }

    /**
     * Builds the argument vector to launch, substituting the received and approved file paths into the
     * template's placeholders. The result is ready to hand to a {@link ProcessBuilder}.
     *
     * <pre>
     * DiffTool meld = new DiffTool("meld", List.of("meld"), List.of("meld", "{received}", "{approved}"));
     * List&lt;String&gt; argv = meld.command("/tmp/x.received.json", "/tmp/x.approved.json");
     * // [meld, /tmp/x.received.json, /tmp/x.approved.json]
     * </pre>
     *
     * @param received the path to the received payload written on drift
     * @param approved the path to the approved snapshot it is compared against
     * @return the launch command with both paths substituted in
     */
    public List<String> command(String received, String approved) {
        var argv = new ArrayList<String>(template.size());
        for (var token : template) {
            argv.add(token.replace("{received}", received).replace("{approved}", approved));
        }
        return List.copyOf(argv);
    }
}
