package finos.traderx.messaging.socketio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SocketIOEnvelopeTest {

    @Test
    void defaultConstructor() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        assertThat(envelope.getTopic()).isNull();
        assertThat(envelope.getPayload()).isNull();
        assertThat(envelope.getType()).isNull();
        assertThat(envelope.getFrom()).isNull();
        assertThat(envelope.getDate()).isNotNull();
    }

    @Test
    void parameterizedConstructorSetsFields() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("/topic", "payload");
        assertThat(envelope.getTopic()).isEqualTo("/topic");
        assertThat(envelope.getPayload()).isEqualTo("payload");
        assertThat(envelope.getType()).isEqualTo("String");
        assertThat(envelope.getDate()).isNotNull();
    }

    @Test
    void setters() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();
        envelope.setTopic("/t");
        envelope.setPayload("p");
        envelope.setType("custom");
        envelope.setFrom("sender");

        assertThat(envelope.getTopic()).isEqualTo("/t");
        assertThat(envelope.getPayload()).isEqualTo("p");
        assertThat(envelope.getType()).isEqualTo("custom");
        assertThat(envelope.getFrom()).isEqualTo("sender");
    }
}
