package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewTests {

    @Test
    void auto_isTheDefaultModeWithNoExplicitTool() {
        var review = SnapshotReview.auto();

        assertEquals(ReviewMode.AUTO, review.mode());
        assertNull(review.tool());
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
        var tool = review.tool();
        assertEquals("meld", tool == null ? null : tool.name());
    }

    @Test
    void toolByName_rejectsAnUnknownName() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> SnapshotReview.tool("no-such-tool"));

        assertTrue(requireNonNull(thrown.getMessage()).contains("no-such-tool"));
    }

    @Test
    void toolByInstance_carriesTheGivenTool() {
        var custom = new DiffTool("acme", List.of("acme"), List.of("acme", "{received}", "{approved}"));

        var review = SnapshotReview.tool(custom);

        assertEquals(ReviewMode.AUTO, review.mode());
        assertSame(custom, review.tool());
    }

    @Test
    void withMode_keepsTheToolAndChangesTheMode() {
        var custom = new DiffTool("acme", List.of("acme"), List.of("acme", "{received}", "{approved}"));

        var review = SnapshotReview.tool(custom).withMode(ReviewMode.OFF);

        assertEquals(ReviewMode.OFF, review.mode());
        assertSame(custom, review.tool());
    }

    @Test
    void withTool_keepsTheModeAndSetsTheTool() {
        var custom = new DiffTool("acme", List.of("acme"), List.of("acme", "{received}", "{approved}"));

        var review = SnapshotReview.approve().withTool(custom);

        assertEquals(ReviewMode.APPROVE, review.mode());
        assertSame(custom, review.tool());
    }
}
