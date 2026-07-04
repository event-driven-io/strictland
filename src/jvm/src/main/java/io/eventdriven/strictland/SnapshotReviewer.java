package io.eventdriven.strictland;

/**
 * Decides what a {@link SnapshotResult} means. Storage only reports; this is the imperative shell that
 * reacts: a match or a first-run approval passes, and a drift either re-baselines (in {@code approve}
 * mode) or opens a diff tool and fails the check with an inline diff of what moved.
 */
final class SnapshotReviewer {

    private final SnapshotReview review;
    private final DiffReview diffReview;

    SnapshotReviewer(SnapshotReview review, DiffReview diffReview) {
        this.review = review;
        this.diffReview = diffReview;
    }

    static SnapshotReviewer forReview(SnapshotReview review) {
        return new SnapshotReviewer(review, DiffReview.forReview(review));
    }

    void review(SnapshotStorage storage, SnapshotResult result) {
        if (!(result instanceof SnapshotResult.Drifted drift)) {
            return;
        }
        if (review.mode() == ReviewMode.APPROVE) {
            storage.approve(drift.location(), drift.received());
            return;
        }
        diffReview.open(drift.receivedFile(), drift.approvedFile());
        throw new AssertionError(driftMessage(drift));
    }

    private static String driftMessage(SnapshotResult.Drifted drift) {
        return "Snapshot drift: " + drift.approvedFile() + " differs from the approved snapshot.\n\n"
                + SnapshotDiff.render(drift.approved().bytes(), drift.received().bytes()) + "\n"
                + "received: " + drift.receivedFile() + "\n"
                + "approved: " + drift.approvedFile() + "\n\n"
                + "To accept this change, re-run with -Dstrictland.review.mode=approve, "
                + "or save the received payload over the approved file in the diff tool.";
    }
}
