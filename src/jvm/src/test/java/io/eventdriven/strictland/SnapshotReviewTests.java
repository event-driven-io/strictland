package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewTests {

    @Test
    void auto_isTheDefaultModeWithNoExplicitTool() {
        var review = SnapshotReview.auto();

        assertEquals(ReviewMode.AUTO, review.mode());
        assertNull(review.toolPreference());
    }

    @Test
    void off_keepsTheDiffButDisablesLaunching() {
        assertEquals(ReviewMode.OFF, SnapshotReview.off().mode());
    }

    @Test
    void approve_selectsTheApproveMode() {
        assertEquals(ReviewMode.APPROVE, SnapshotReview.approve().mode());
    }

    @Test
    void tool_selectsAKnownToolInAutoMode() {
        var review = SnapshotReview.tool(DiffTool.MELD);

        assertEquals(ReviewMode.AUTO, review.mode());
        assertEquals(new ToolPreference.Named(DiffTool.MELD), review.toolPreference());
    }

    @Test
    void customTool_selectsACustomCommandInAutoMode() {
        var review = SnapshotReview.customTool("my-diff {received} {approved}");

        assertEquals(ReviewMode.AUTO, review.mode());
        assertEquals(new ToolPreference.Custom("my-diff {received} {approved}"), review.toolPreference());
    }

    @Test
    void customTool_rejectsABlankCommand() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> SnapshotReview.customTool("   "));

        assertTrue(requireNonNull(thrown.getMessage()).contains("blank"));
    }

    @Test
    void equalityAndHashCodeAreValueBased() {
        var left = SnapshotReview.tool(DiffTool.IDEA);
        var right = SnapshotReview.tool(DiffTool.IDEA);

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertEquals(left.toString(), right.toString());
    }

    @Test
    void equalityHandlesIdentityOtherTypesAndDifferentValues() {
        var review = SnapshotReview.tool(DiffTool.MELD);

        assertEquals(review, review);
        assertNotEquals(review, "meld");
        assertNotEquals(review, SnapshotReview.off());
        assertNotEquals(review, SnapshotReview.tool(DiffTool.IDEA));
    }
}
