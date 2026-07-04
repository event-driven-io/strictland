package io.eventdriven.strictland;

import java.nio.file.Path;

/**
 * Decides what the check should do with the outcome of storing a snapshot, without doing it. A {@code
 * then} step asks for the verdict right after storing and carries it out itself: a first run or an
 * unchanged match {@link Verdict.Passed passes}, a drift under {@code approve} review mode asks to
 * {@link Verdict.ReBaseline re-baseline} the approved snapshot, and any other drift asks to {@link
 * Verdict.Fail fail} the check with a ready-to-read diff of what changed.
 */
final class SnapshotReviewer {

    private final SnapshotReview review;

    SnapshotReviewer(SnapshotReview review) {
        this.review = review;
    }

    Verdict decide(SnapshotResult result) {
        if (!(result instanceof SnapshotResult.Drifted drift)) {
            return new Verdict.Passed();
        }
        if (review.mode() == ReviewMode.APPROVE) {
            return new Verdict.ReBaseline(drift.location(), drift.received());
        }
        return new Verdict.Fail(driftMessage(drift), drift.receivedFile(), drift.approvedFile());
    }

    private static String driftMessage(SnapshotResult.Drifted drift) {
        return "Snapshot drift: " + drift.approvedFile() + " differs from the approved snapshot.\n\n"
                + SnapshotDiff.render(drift.approved().bytes(), drift.received().bytes()) + "\n"
                + "received: " + drift.receivedFile() + "\n"
                + "approved: " + drift.approvedFile() + "\n\n"
                + "To accept this change, re-run with -Dstrictland.review.mode=approve, "
                + "or save the received payload over the approved file in the diff tool.";
    }

    /** What a {@code then} step should do once a snapshot has been stored. */
    sealed interface Verdict {

        /** The snapshot held - a first run or an unchanged match - so the check passes. */
        record Passed() implements Verdict {}

        /** A drift accepted under {@code approve} mode: overwrite the approved snapshot with the payload. */
        record ReBaseline(SnapshotLocation location, SnapshotData data) implements Verdict {}

        /** A drift to reject: open the pair in the diff tool, then fail the check with {@code message}. */
        record Fail(String message, Path receivedFile, Path approvedFile) implements Verdict {}
    }
}
