package finos.traderx.messaging.socketio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;

class SocketIOEnvelopeTest {

    @Test
    void constructor_withTopicAndPayload_setsFieldsCorrectly() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("test-topic", "hello");

        assertThat(envelope.getTopic()).isEqualTo("test-topic");
        assertThat(envelope.getPayload()).isEqualTo("hello");
        assertThat(envelope.getType()).isEqualTo("String");
        assertThat(envelope.getDate()).isNotNull();
    }

    @Test
    void defaultConstructor_createsEmptyEnvelope() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();

        assertThat(envelope.getTopic()).isNull();
        assertThat(envelope.getPayload()).isNull();
        assertThat(envelope.getType()).isNull();
        assertThat(envelope.getFrom()).isNull();
    }

    @Test
    void setters_allFields_updateCorrectly() {
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>();

        envelope.setTopic("/trades");
        envelope.setPayload("order-data");
        envelope.setType("TradeOrder");
        envelope.setFrom("trade-service");

        assertThat(envelope.getTopic()).isEqualTo("/trades");
        assertThat(envelope.getPayload()).isEqualTo("order-data");
        assertThat(envelope.getType()).isEqualTo("TradeOrder");
        assertThat(envelope.getFrom()).isEqualTo("trade-service");
    }

    @Test
    void getDate_afterConstruction_returnsNonNullDate() {
        long before = System.currentTimeMillis();
        SocketIOEnvelope<String> envelope = new SocketIOEnvelope<>("topic", "payload");
        long after = System.currentTimeMillis();

        assertThat(envelope.getDate()).isNotNull();
        assertThat(envelope.getDate().getTime()).isBetween(before, after);
    }

    @Test
    void constructor_withPayload_setsTypeFromPayloadClassName() {
        SocketIOEnvelope<Integer> envelope = new SocketIOEnvelope<>("/numbers", 42);

        assertThat(envelope.getType()).isEqualTo("Integer");
    }
}
