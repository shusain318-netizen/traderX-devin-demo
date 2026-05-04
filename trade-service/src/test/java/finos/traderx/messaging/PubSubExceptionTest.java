package finos.traderx.messaging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PubSubExceptionTest {

    @Test
    void constructorWithMessage() {
        PubSubException ex = new PubSubException("error");
        assertThat(ex.getMessage()).isEqualTo("error");
    }

    @Test
    void constructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root");
        PubSubException ex = new PubSubException("wrapped", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void constructorWithCauseOnly() {
        Throwable cause = new RuntimeException("root cause");
        PubSubException ex = new PubSubException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
