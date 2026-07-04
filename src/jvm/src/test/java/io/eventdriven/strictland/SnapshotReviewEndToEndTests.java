package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class SnapshotReviewEndToEndTests {

    private record ReviewOrder(String orderId, String customer) {}

    private static SpecificationOptions options(Path root) {
        return options(root, SnapshotReview.off());
    }

    private static SpecificationOptions options(Path root, SnapshotReview review) {
        return Json.Jackson.defaults()
                .snapshotLayout(SnapshotLayout.registry().rootPath(root.toString()))
                .snapshotReview(review);
    }

    private static MessageSnapshot snapshot() {
        return MessageSnapshot.ofTypeNamed("review.ReviewOrder");
    }

    private static Path approvedFile(Path root) {
        return root.resolve("contract-registry")
                .resolve("review")
                .resolve("ReviewOrder")
                .resolve("ReviewOrder.1.default.snap.approved.json");
    }

    private static Path receivedFile(Path root) {
        return root.resolve("contract-registry")
                .resolve("review")
                .resolve("ReviewOrder")
                .resolve("ReviewOrder.1.default.snap.received.json");
    }

    @Test
    void contractCheck_whenSerializedPayloadDrifts_writesReceivedAndReportsInlineDiff(@TempDir Path root)
            throws Exception {
        MessageContract.specification(options(root))
                .given(new ReviewOrder("order-1", "Alice"))
                .whenSerializedAs(snapshot())
                .thenContractIsUnchanged();

        var error = assertThrows(
                AssertionError.class,
                () -> MessageContract.specification(options(root))
                        .given(new ReviewOrder("order-1", "Bob"))
                        .whenSerializedAs(snapshot())
                        .thenContractIsUnchanged());

        assertArrayEquals(
                "{\"orderId\":\"order-1\",\"customer\":\"Alice\"}".getBytes(UTF_8),
                Files.readAllBytes(approvedFile(root)));
        assertArrayEquals(
                "{\"orderId\":\"order-1\",\"customer\":\"Bob\"}".getBytes(UTF_8),
                Files.readAllBytes(receivedFile(root)));
        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("Text content differs (- approved, + received):"), message);
        assertTrue(message.contains("- 1 | {\"orderId\":\"order-1\",\"customer\":\"Alice\"}"), message);
        assertTrue(message.contains("+ 1 | {\"orderId\":\"order-1\",\"customer\":\"Bob\"}"), message);
        assertTrue(message.contains("received: " + receivedFile(root)), message);
        assertTrue(message.contains("approved: " + approvedFile(root)), message);
    }

    @Test
    void contractCheck_inApproveMode_whenPayloadDrifts_reBaselinesTheApprovedFileWithoutFailing(@TempDir Path root)
            throws Exception {
        MessageContract.specification(options(root))
                .given(new ReviewOrder("order-1", "Alice"))
                .whenSerializedAs(snapshot())
                .thenContractIsUnchanged();

        MessageContract.specification(options(root, SnapshotReview.approve()))
                .given(new ReviewOrder("order-1", "Bob"))
                .whenSerializedAs(snapshot())
                .thenContractIsUnchanged();

        assertArrayEquals(
                "{\"orderId\":\"order-1\",\"customer\":\"Bob\"}".getBytes(UTF_8),
                Files.readAllBytes(approvedFile(root)));
        assertFalse(Files.exists(receivedFile(root)));
    }
}
