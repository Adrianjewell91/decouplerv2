package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.ExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionExceptionTest {

    @Test
    void message_only_constructor() {
        var ex = new ExecutionException("fragment returned no rows");

        assertThat(ex.getMessage()).isEqualTo("fragment returned no rows");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void message_and_cause_constructor() {
        var cause = new RuntimeException("connection refused");
        var ex = new ExecutionException("execution failed", cause);

        assertThat(ex.getMessage()).isEqualTo("execution failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void is_checked_exception() {
        assertThat(new ExecutionException("x")).isInstanceOf(Exception.class)
                                               .isNotInstanceOf(RuntimeException.class);
    }
}
