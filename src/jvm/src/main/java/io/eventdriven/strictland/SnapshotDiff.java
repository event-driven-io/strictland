package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.HexFormat;
import java.util.List;

/**
 * Renders the human-readable difference between an approved snapshot and the drifted received payload,
 * the body {@link FileSnapshotStorage} embeds in a drift's {@code AssertionError}. Text payloads (JSON,
 * CSV, and the like) get a line-based unified diff; binary payloads fall back to a byte-length and
 * short hex summary. Package-private: it is an internal detail of the failure message.
 */
final class SnapshotDiff {

    private static final int HEX_PREVIEW_BYTES = 16;

    private SnapshotDiff() {}

    /**
     * Renders the difference between the two payloads, choosing a unified line diff when both decode as
     * text and a hex/length summary otherwise.
     */
    static String render(byte[] approved, byte[] received) {
        if (isText(approved) && isText(received)) {
            return unifiedDiff(lines(approved), lines(received));
        }
        return binarySummary(approved, received);
    }

    private static List<String> lines(byte[] bytes) {
        return List.of(new String(bytes, UTF_8).split("\n", -1));
    }

    private static String unifiedDiff(List<String> approved, List<String> received) {
        var n = approved.size();
        var m = received.size();
        var lcs = new int[n + 1][m + 1];
        for (var i = n - 1; i >= 0; i--) {
            for (var j = m - 1; j >= 0; j--) {
                lcs[i][j] = approved.get(i).equals(received.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        var out = new StringBuilder();
        var i = 0;
        var j = 0;
        while (i < n && j < m) {
            if (approved.get(i).equals(received.get(j))) {
                out.append("  ").append(approved.get(i)).append('\n');
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.append("- ").append(approved.get(i)).append('\n');
                i++;
            } else {
                out.append("+ ").append(received.get(j)).append('\n');
                j++;
            }
        }
        while (i < n) {
            out.append("- ").append(approved.get(i)).append('\n');
            i++;
        }
        while (j < m) {
            out.append("+ ").append(received.get(j)).append('\n');
            j++;
        }
        return out.toString();
    }

    private static String binarySummary(byte[] approved, byte[] received) {
        return "Binary content differs (- approved, + received):\n"
                + "- " + approved.length + " bytes " + hexPreview(approved) + "\n"
                + "+ " + received.length + " bytes " + hexPreview(received);
    }

    private static String hexPreview(byte[] bytes) {
        var limit = Math.min(bytes.length, HEX_PREVIEW_BYTES);
        var hex = HexFormat.ofDelimiter(" ").formatHex(bytes, 0, limit);
        return bytes.length > limit ? "[" + hex + " ...]" : "[" + hex + "]";
    }

    private static boolean isText(byte[] bytes) {
        for (var b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        try {
            UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
