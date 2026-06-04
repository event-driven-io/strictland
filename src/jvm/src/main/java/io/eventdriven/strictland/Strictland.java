package io.eventdriven.strictland;

import org.jspecify.annotations.NullMarked;

/**
 * Entry point for the Strictland library.
 */
@NullMarked
public final class Strictland {

    /**
     * Returns a greeting message.
     *
     * @return greeting string
     */
    public String greet() {
        return "Hello from Strictland!";
    }
}
