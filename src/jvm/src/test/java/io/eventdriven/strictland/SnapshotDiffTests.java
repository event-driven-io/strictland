package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotDiffTests {

    private static byte[] utf8(String text) {
        return text.getBytes(UTF_8);
    }

    @Test
    void text_marksAChangedLineAsRemovedThenAdded() {
        var diff = SnapshotDiff.render(utf8("a\nb\nc"), utf8("a\nx\nc"));

        assertEquals("""
                  a
                - b
                + x
                  c
                """, diff);
    }

    @Test
    void text_marksTrailingAddedLines() {
        var diff = SnapshotDiff.render(utf8("a"), utf8("a\nb"));

        assertEquals("""
                  a
                + b
                """, diff);
    }

    @Test
    void text_marksTrailingRemovedLines() {
        var diff = SnapshotDiff.render(utf8("a\nb"), utf8("a"));

        assertEquals("""
                  a
                - b
                """, diff);
    }

    @Test
    void text_prefersRemovalWhenApprovedIsLongerAtADivergence() {
        var diff = SnapshotDiff.render(utf8("a\nb\nc\nd"), utf8("a\nd"));

        assertEquals("""
                  a
                - b
                - c
                  d
                """, diff);
    }

    @Test
    void binary_fallsBackToLengthAndHexPreviewWhenPayloadHasNulBytes() {
        var approved = new byte[] {0, 1, 2};
        var received = new byte[] {0, 1, 2, 3};

        var diff = SnapshotDiff.render(approved, received);

        assertTrue(diff.startsWith("Binary content differs"), diff);
        assertTrue(diff.contains("- 3 bytes [00 01 02]"), diff);
        assertTrue(diff.contains("+ 4 bytes [00 01 02 03]"), diff);
    }

    @Test
    void binary_truncatesTheHexPreviewBeyondSixteenBytes() {
        var twenty = new byte[20];
        twenty[0] = 0; // force the binary branch

        var diff = SnapshotDiff.render(twenty, new byte[] {0});

        assertTrue(diff.contains("...]"), diff);
    }

    @Test
    void mixed_textApprovedButBinaryReceived_fallsBackToBinary() {
        var diff = SnapshotDiff.render(utf8("a"), new byte[] {0, 1});

        assertTrue(diff.startsWith("Binary content differs"), diff);
    }

    @Test
    void binary_detectsInvalidUtf8AsBinary() {
        var invalidUtf8 = new byte[] {(byte) 0xC3, (byte) 0x28};

        var diff = SnapshotDiff.render(invalidUtf8, invalidUtf8);

        assertTrue(diff.startsWith("Binary content differs"), diff);
    }
}
