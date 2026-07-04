package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewPropertiesTests {

    private static Properties props(String key, String value) {
        var props = new Properties();
        props.setProperty(key, value);
        return props;
    }

    @Test
    void noReviewKeys_isEmptySoTheCallerFallsThrough() {
        assertEquals(Optional.empty(), SnapshotReviewProperties.fromProperties(new Properties()));
    }

    @Test
    void mode_isParsedForEachValueCaseInsensitively() {
        assertEquals(
                ReviewMode.AUTO,
                SnapshotReviewProperties.fromProperties(props("strictland.review.mode", "auto"))
                        .orElseThrow()
                        .mode());
        assertEquals(
                ReviewMode.OFF,
                SnapshotReviewProperties.fromProperties(props("strictland.review.mode", "OFF"))
                        .orElseThrow()
                        .mode());
        assertEquals(
                ReviewMode.APPROVE,
                SnapshotReviewProperties.fromProperties(props("strictland.review.mode", "approve"))
                        .orElseThrow()
                        .mode());
    }

    @Test
    void anUnknownMode_isRejected() {
        var thrown = assertThrows(
                IllegalArgumentException.class,
                () -> SnapshotReviewProperties.fromProperties(props("strictland.review.mode", "yolo")));

        assertTrue(requireNonNull(thrown.getMessage()).contains("yolo"));
    }

    @Test
    void tool_byRegistryName_isResolved() {
        var review = SnapshotReviewProperties.fromProperties(props("strictland.review.tool", "meld"))
                .orElseThrow();

        assertEquals(ReviewMode.AUTO, review.mode());
        assertEquals("meld", requireNonNull(review.tool()).name());
    }

    @Test
    void tool_asAFullTemplate_buildsACustomTool() {
        var review = SnapshotReviewProperties.fromProperties(
                        props("strictland.review.tool", "my-diff {received} {approved}"))
                .orElseThrow();

        assertEquals("custom", requireNonNull(review.tool()).name());
    }

    @Test
    void tool_thatIsBlank_isIgnored() {
        var review = SnapshotReviewProperties.fromProperties(props("strictland.review.tool", "   "))
                .orElseThrow();

        assertNull(review.tool());
    }

    @Test
    void tool_thatIsUnknown_isRejected() {
        var thrown = assertThrows(
                IllegalArgumentException.class,
                () -> SnapshotReviewProperties.fromProperties(props("strictland.review.tool", "no-such-tool")));

        assertTrue(requireNonNull(thrown.getMessage()).contains("no-such-tool"));
    }

    @Test
    void fromClasspath_whenResourceIsPresent_parsesModeAndTool() {
        var review = SnapshotReviewProperties.fromClasspath("fixtures/review-sample.properties")
                .orElseThrow();

        assertEquals(ReviewMode.APPROVE, review.mode());
        assertEquals("meld", requireNonNull(review.tool()).name());
    }

    @Test
    void fromClasspath_whenResourceIsAbsent_returnsEmpty() {
        assertEquals(Optional.empty(), SnapshotReviewProperties.fromClasspath());
    }

    @Test
    void fromStream_whenStreamIsNull_returnsEmpty() {
        assertEquals(Optional.empty(), SnapshotReviewProperties.fromStream("nowhere", null));
    }

    @Test
    void fromStream_whenReadingFails_wrapsTheIoException() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                throw new IOException("boom");
            }
        };

        var thrown =
                assertThrows(UncheckedIOException.class, () -> SnapshotReviewProperties.fromStream("failing", failing));

        assertTrue(requireNonNull(thrown.getMessage()).contains("failing"));
    }
}
