package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewerTests {

    private static final MessageTypeName MESSAGE_TYPE = MessageTypeName.of("io.eventdriven.strictland.OrderPlaced");
    private static final SnapshotLocation LOCATION =
            SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");
    private static final Path RECEIVED_FILE = Path.of("OrderPlaced.1.default.snap.received.json");
    private static final Path APPROVED_FILE = Path.of("OrderPlaced.1.default.snap.approved.json");

    private static SnapshotResult.Drifted drift(byte[] approved, byte[] received) {
        return new SnapshotResult.Drifted(
                LOCATION, new SnapshotData(approved), new SnapshotData(received), APPROVED_FILE, RECEIVED_FILE);
    }

    @Test
    void aFirstRunApprovalPasses() {
        var verdict = new SnapshotReviewer(SnapshotReview.auto()).decide(new SnapshotResult.Approved(LOCATION));

        assertInstanceOf(SnapshotReviewer.Verdict.Passed.class, verdict);
    }

    @Test
    void anUnchangedMatchPasses() {
        var verdict = new SnapshotReviewer(SnapshotReview.auto()).decide(new SnapshotResult.Unchanged(LOCATION));

        assertInstanceOf(SnapshotReviewer.Verdict.Passed.class, verdict);
    }

    @Test
    void aDriftFailsCarryingTheReceivedAndApprovedFilesForTheCallerToOpen() {
        var verdict = new SnapshotReviewer(SnapshotReview.auto())
                .decide(drift("{\"id\":1}".getBytes(UTF_8), "{\"id\":2}".getBytes(UTF_8)));

        var fail = assertInstanceOf(SnapshotReviewer.Verdict.Fail.class, verdict);
        assertEquals(RECEIVED_FILE, fail.receivedFile());
        assertEquals(APPROVED_FILE, fail.approvedFile());
    }

    @Test
    void aDriftFailureCarriesTheInlineDiffBothPathsAndTheApproveHint() {
        var verdict = new SnapshotReviewer(SnapshotReview.off())
                .decide(drift("{\"id\":1}".getBytes(UTF_8), "{\"id\":2}".getBytes(UTF_8)));

        var message =
                assertInstanceOf(SnapshotReviewer.Verdict.Fail.class, verdict).message();
        assertTrue(message.contains("Text content differs (- approved, + received):"), message);
        assertTrue(message.contains("- 1 | {\"id\":1}"), message);
        assertTrue(message.contains("+ 1 | {\"id\":2}"), message);
        assertTrue(message.contains("received: " + RECEIVED_FILE), message);
        assertTrue(message.contains("approved: " + APPROVED_FILE), message);
        assertTrue(message.contains("-Dstrictland.review.mode=approve"), message);
    }

    @Test
    void aDriftFailureFallsBackToAHexSummaryForBinaryPayloads() {
        var verdict =
                new SnapshotReviewer(SnapshotReview.off()).decide(drift(new byte[] {0, 1, 2}, new byte[] {0, 1, 3}));

        var message =
                assertInstanceOf(SnapshotReviewer.Verdict.Fail.class, verdict).message();
        assertTrue(message.contains("Binary content differs"), message);
    }

    @Test
    void approveModeAsksToReBaselineWithTheReceivedPayload() {
        var received = "{\"id\":2}".getBytes(UTF_8);

        var verdict =
                new SnapshotReviewer(SnapshotReview.approve()).decide(drift("{\"id\":1}".getBytes(UTF_8), received));

        var reBaseline = assertInstanceOf(SnapshotReviewer.Verdict.ReBaseline.class, verdict);
        assertEquals(LOCATION, reBaseline.location());
        assertArrayEquals(received, reBaseline.data().bytes());
    }
}
