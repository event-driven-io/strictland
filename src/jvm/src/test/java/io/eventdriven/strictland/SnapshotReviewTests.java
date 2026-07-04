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
    void toolByName_selectsAKnownToolInAutoMode() {
        var review = SnapshotReview.tool("meld");

        assertEquals(ReviewMode.AUTO, review.mode());
        var preference = requireNonNull(review.toolPreference());
        assertEquals(ToolPreference.Kind.SINGLE, preference.kind());
        assertEquals(java.util.List.of("meld"), preference.names());
    }

    @Test
    void toolOrder_selectsPreferredRegisteredToolsInAutoMode() {
        var review = SnapshotReview.toolOrder("idea", "vscode");

        assertEquals(ReviewMode.AUTO, review.mode());
        var preference = requireNonNull(review.toolPreference());
        assertEquals(ToolPreference.Kind.ORDER, preference.kind());
        assertEquals(java.util.List.of("idea", "vscode"), preference.names());
    }

    @Test
    void unknownToolNamesAreRejected() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> SnapshotReview.tool("no-such-tool"));

        assertTrue(requireNonNull(thrown.getMessage()).contains("no-such-tool"));
    }

    @Test
    void unknownToolOrderNamesAreRejected() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> SnapshotReview.toolOrder("meld", "nope"));

        assertTrue(requireNonNull(thrown.getMessage()).contains("nope"));
    }

    @Test
    void emptyToolOrderIsRejected() {
        var thrown = assertThrows(IllegalArgumentException.class, SnapshotReview::toolOrder);

        assertTrue(requireNonNull(thrown.getMessage()).contains("at least one"));
    }

    @Test
    void equalityAndHashCodeAreValueBased() {
        var left = SnapshotReview.toolOrder("idea", "meld");
        var right = SnapshotReview.toolOrder("idea", "meld");

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertEquals(left.toString(), right.toString());
    }

    @Test
    void equalityHandlesIdentityOtherTypesAndDifferentValues() {
        var review = SnapshotReview.tool("meld");

        assertEquals(review, review);
        assertNotEquals(review, "meld");
        assertNotEquals(review, SnapshotReview.off());
        assertNotEquals(review, SnapshotReview.tool("idea"));
    }

    @Test
    void internalToolPreferenceRejectsInvalidStates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolPreference(ToolPreference.Kind.SINGLE, java.util.List.of("meld"), "template"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolPreference(ToolPreference.Kind.CUSTOM, java.util.List.of(), "   "));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolPreference(ToolPreference.Kind.ORDER, java.util.List.of(), null));
    }
}
