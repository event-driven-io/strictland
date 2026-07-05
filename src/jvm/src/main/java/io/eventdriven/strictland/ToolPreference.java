package io.eventdriven.strictland;

/**
 * Which diff tool auto mode should launch: a specific one Strictland knows, or a custom command line.
 * A {@code null} preference means "try the known tools in their built-in order".
 */
sealed interface ToolPreference {

    /** A specific known tool, chosen by the caller. */
    record Named(DiffTool tool) implements ToolPreference {}

    /**
     * A custom command line, with {@code {received}} and {@code {approved}} placeholders for the two
     * files, for a tool Strictland does not know.
     */
    record Custom(String command) implements ToolPreference {
        public Custom {
            if (command.isBlank()) {
                throw new IllegalArgumentException("Custom diff command must not be blank.");
            }
        }
    }
}
