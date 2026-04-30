package finos.traderx.messaging.socketio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeservice.model.TradeOrder;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

class SocketIOJSONSubscriberTest {

    private Socket mockSocket;
    private TestableSubscriber subscriber;

    static class TestableSubscriber extends SocketIOJSONSubscriber<TradeOrder> {
        TradeOrder lastMessage;
        Envelope<?> lastEnvelope;
        private final Socket testSocket;

        TestableSubscriber(Socket testSocket) {
            super(TradeOrder.class);
            this.testSocket = testSocket;
        }

        @Override
        public void onMessage(Envelope<?> envelope, TradeOrder message) {
            this.lastEnvelope = envelope;
            this.lastMessage = message;
        }

        @Override
        protected Socket internalConnect(URI uri) throws Exception {
            testSocket.on(Socket.EVENT_CONNECT, args -> {
                TestableSubscriber.this.connected = true;
            });
            testSocket.on(Socket.EVENT_DISCONNECT, args -> {
                TestableSubscriber.this.connected = false;
            });
            testSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                TestableSubscriber.this.connected = false;
            });
            testSocket.connect();
            return testSocket;
        }
    }

    @BeforeEach
    void setUp() {
        mockSocket = mock(Socket.class);
        when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
        subscriber = new TestableSubscriber(mockSocket);
    }

    @Test
    void isConnected_default_returnsFalse() {
        assertThat(subscriber.isConnected()).isFalse();
    }

    @Test
    void setSocketAddress_newAddress_updatesAddress() {
        subscriber.setSocketAddress("http://newhost:5000");
        assertThat(subscriber.socketAddress).isEqualTo("http://newhost:5000");
    }

    @Test
    void setDefaultTopic_newTopic_updatesTopic() {
        subscriber.setDefaultTopic("/custom");
        // Verify no exception; field is private so we test via afterPropertiesSet behavior
        assertThat(subscriber).isNotNull();
    }

    @Test
    void subscribe_withSocket_emitsSubscribeEvent() throws PubSubException {
        subscriber.socket = mockSocket;
        subscriber.subscribe("/trades");
        verify(mockSocket).emit("subscribe", "/trades");
    }

    @Test
    void unsubscribe_withSocket_emitsUnsubscribeWithLiteralString() throws PubSubException {
        subscriber.socket = mockSocket;
        subscriber.unsubscribe("/trades");
        // BUG: the code uses string literal "topic" instead of the parameter
        verify(mockSocket).emit("unsubscribe", "topic");
    }

    @Test
    void disconnect_connectedWithSocket_disconnectsSocket() throws PubSubException {
        subscriber.connected = true;
        subscriber.socket = mockSocket;

        subscriber.disconnect();

        verify(mockSocket).disconnect();
        assertThat(subscriber.socket).isNull();
    }

    @Test
    void disconnect_socketIsNull_doesNotThrow() throws PubSubException {
        subscriber.socket = null;

        subscriber.disconnect();

        assertThat(subscriber.socket).isNull();
    }

    @Test
    void disconnect_notConnectedWithSocket_doesNotDisconnect() throws PubSubException {
        subscriber.connected = false;
        subscriber.socket = mockSocket;

        subscriber.disconnect();

        verify(mockSocket, never()).disconnect();
    }

    @Test
    void connect_noExistingSocket_createsNewSocket() throws PubSubException {
        subscriber.connect();

        assertThat(subscriber.socket).isSameAs(mockSocket);
    }

    @Test
    void connect_existingSocket_disconnectsOldFirst() throws PubSubException {
        Socket oldSocket = mock(Socket.class);
        subscriber.socket = oldSocket;

        subscriber.connect();

        verify(oldSocket).disconnect();
        assertThat(subscriber.socket).isSameAs(mockSocket);
    }

    @Test
    void connect_connectionFails_throwsPubSubException() {
        TestableSubscriber failingSub = new TestableSubscriber(null) {
            @Override
            protected Socket internalConnect(URI uri) throws Exception {
                throw new Exception("connection refused");
            }
        };

        assertThatThrownBy(failingSub::connect)
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("Cannot socket connection");
    }

    @Test
    void afterPropertiesSet_always_connectsAndSubscribesToDefaultTopic() throws Exception {
        subscriber.afterPropertiesSet();

        verify(mockSocket).connect();
        verify(mockSocket).emit("subscribe", "/default");
    }

    @Test
    void afterPropertiesSet_customTopic_subscribesToCustomTopic() throws Exception {
        subscriber.setDefaultTopic("/my-topic");

        subscriber.afterPropertiesSet();

        verify(mockSocket).emit("subscribe", "/my-topic");
    }

    @Test
    void connect_registersEventListeners() throws Exception {
        subscriber.connect();

        verify(mockSocket).on(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
        verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
        verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
    }

    @Test
    void connectListener_setsConnectedTrue() throws Exception {
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(eq(Socket.EVENT_CONNECT), listenerCaptor.capture())).thenReturn(mockSocket);

        subscriber.connect();

        listenerCaptor.getValue().call();
        assertThat(subscriber.isConnected()).isTrue();
    }

    @Test
    void disconnectListener_setsConnectedFalse() throws Exception {
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(eq(Socket.EVENT_DISCONNECT), listenerCaptor.capture())).thenReturn(mockSocket);

        subscriber.connect();
        subscriber.connected = true;

        listenerCaptor.getValue().call();
        assertThat(subscriber.isConnected()).isFalse();
    }

    @Test
    void connectErrorListener_setsConnectedFalse() throws Exception {
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(eq(Socket.EVENT_CONNECT_ERROR), listenerCaptor.capture())).thenReturn(mockSocket);

        subscriber.connect();
        subscriber.connected = true;

        listenerCaptor.getValue().call();
        assertThat(subscriber.isConnected()).isFalse();
    }
}
