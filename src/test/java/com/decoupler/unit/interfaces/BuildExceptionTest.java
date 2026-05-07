package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.BuildException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BuildException: message and cause chaining.
 */
class BuildExceptionTest {

    @Test
    void message_only_constructor() {
        var ex = new BuildException("LLM returned invalid JSON");

        assertThat(ex.getMessage()).isEqualTo("LLM returned invalid JSON");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void message_and_cause_constructor() {
        var cause = new RuntimeException("parse failure");
        var ex = new BuildException("Could not build aggregator", cause);

        assertThat(ex.getMessage()).isEqualTo("Could not build aggregator");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void is_checked_exception() {
        assertThat(new BuildException("x")).isInstanceOf(Exception.class)
                                           .isNotInstanceOf(RuntimeException.class);
    }
}
