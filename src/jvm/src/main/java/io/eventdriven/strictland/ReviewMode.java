package io.eventdriven.strictland;

/**
 * What Strictland does when a snapshot check finds the message no longer matches its approved
 * baseline. It is the single knob behind {@link SnapshotReview}: the failure message always carries an
 * inline diff, and the mode decides whether a diff tool opens and whether the drift fails the build or
 * re-baselines the snapshot.
 *
 * <pre>
 * SpecificationOptions options = Json.Jackson.defaults().snapshotReview(SnapshotReview.approve());
 * </pre>
 */
public enum ReviewMode {

    /**
     * The default: render the inline diff in the failure message, and on a local, interactive machine
     * also open a detected diff tool showing the received payload against the approved one. On CI or a
     * headless machine no tool opens and the inline diff is the whole story.
     */
    AUTO,

    /**
     * Render the inline diff in the failure message, but never open a diff tool. Reach for it when a
     * tool would get in the way, for instance to keep a local run as quiet as CI.
     */
    OFF,

    /**
     * On drift, promote the received payload over the approved snapshot instead of failing, so you
     * re-baseline a message you changed on purpose. Set it for a run with {@code
     * -Dstrictland.review.mode=approve} to accept the snapshots that run touches.
     */
    APPROVE
}
