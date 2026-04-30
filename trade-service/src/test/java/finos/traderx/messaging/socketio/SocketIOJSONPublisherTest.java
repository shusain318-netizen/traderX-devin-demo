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

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

class SocketIOJSONPublisherTest {

    private Socket mockSocket;
    private TestablePublisher publisher;

    static class TestablePublisher extends SocketIOJSONPublisher<TradeOrder> {
        private final Socket testSocket;

        TestablePublisher(Socket testSocket) {
            this.testSocket = testSocket;
        }

        @Override
        protected Socket internalConnect(URI uri) throws Exception {
            return testSocket;
        }
    }

    @BeforeEach
    void setUp() {
        mockSocket = mock(Socket.class);
        publisher = new TestablePublisher(mockSocket);
    }

    @Test
    void isConnected_default_returnsFalse() {
        assertThat(publisher.isConnected()).isFalse();
    }

    @Test
    void setSocketAddress_newAddress_updatesAddress() {
        publisher.setSocketAddress("http://newhost:9090");
        assertThat(publisher.socketAddress).isEqualTo("http://newhost:9090");
    }

    @Test
    void setTopic_newTopic_updatesTopic() {
        publisher.setTopic("/custom-topic");
        assertThat(publisher.topic).isEqualTo("/custom-topic");
    }

    @Test
    void publish_notConnected_throwsPubSubException() {
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);

        assertThatThrownBy(() -> publisher.publish("/trades", order))
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("not connected");
    }

    @Test
    void publishDefaultTopic_notConnected_throwsPubSubException() {
        publisher.setTopic("/my-topic");
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);

        assertThatThrownBy(() -> publisher.publish(order))
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("not connected");
    }

    @Test
    void publish_connected_emitsOnSocket() throws PubSubException {
        publisher.connected = true;
        publisher.socket = mockSocket;
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);

        publisher.publish("/trades", order);

        verify(mockSocket).emit(eq("publish"), any(JSONObject.class));
    }

    @Test
    void publishDefaultTopic_connected_usesConfiguredTopic() throws PubSubException {
        publisher.connected = true;
        publisher.socket = mockSocket;
        publisher.setTopic("/custom");
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);

        publisher.publish(order);

        verify(mockSocket).emit(eq("publish"), any(JSONObject.class));
    }

    @Test
    void disconnect_connectedWithSocket_disconnectsSocket() throws PubSubException {
        publisher.connected = true;
        publisher.socket = mockSocket;

        publisher.disconnect();

        verify(mockSocket).disconnect();
        assertThat(publisher.socket).isNull();
    }

    @Test
    void disconnect_socketIsNull_doesNotThrow() throws PubSubException {
        publisher.socket = null;

        publisher.disconnect();

        assertThat(publisher.socket).isNull();
    }

    @Test
    void disconnect_notConnectedWithSocket_doesNotDisconnect() throws PubSubException {
        publisher.connected = false;
        publisher.socket = mockSocket;

        publisher.disconnect();

        verify(mockSocket, never()).disconnect();
    }

    @Test
    void connect_noExistingSocket_createsNewSocket() throws PubSubException {
        publisher.connect();

        assertThat(publisher.socket).isSameAs(mockSocket);
    }

    @Test
    void connect_existingSocket_disconnectsOldFirst() throws PubSubException {
        Socket oldSocket = mock(Socket.class);
        publisher.socket = oldSocket;

        publisher.connect();

        verify(oldSocket).disconnect();
        assertThat(publisher.socket).isSameAs(mockSocket);
    }

    @Test
    void connect_connectionFails_throwsPubSubException() {
        TestablePublisher failingPublisher = new TestablePublisher(null) {
            @Override
            protected Socket internalConnect(URI uri) throws Exception {
                throw new Exception("connection refused");
            }
        };

        assertThatThrownBy(failingPublisher::connect)
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("Cannot socket connection");
    }

    @Test
    void afterPropertiesSet_always_connectsAndRegistersListeners() throws Exception {
        when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);

        publisher.afterPropertiesSet();

        verify(mockSocket).on(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
        verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
        verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
        verify(mockSocket).connect();
    }

    @Test
    void afterPropertiesSet_connectEvent_setsConnectedTrue() throws Exception {
        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(eventCaptor.capture(), listenerCaptor.capture())).thenReturn(mockSocket);

        publisher.afterPropertiesSet();

        List<String> events = eventCaptor.getAllValues();
        List<Emitter.Listener> listeners = listenerCaptor.getAllValues();
        int idx = events.indexOf(Socket.EVENT_CONNECT);
        listeners.get(idx).call();

        assertThat(publisher.isConnected()).isTrue();
    }

    @Test
    void afterPropertiesSet_disconnectEvent_setsConnectedFalse() throws Exception {
        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(eventCaptor.capture(), listenerCaptor.capture())).thenReturn(mockSocket);

        publisher.afterPropertiesSet();
        publisher.connected = true;

        List<String> events = eventCaptor.getAllValues();
        List<Emitter.Listener> listeners = listenerCaptor.getAllValues();
        int idx = events.indexOf(Socket.EVENT_DISCONNECT);
        listeners.get(idx).call();

        assertThat(publisher.isConnected()).isFalse();
    }

    @Test
    void afterPropertiesSet_connectErrorEvent_setsConnectedFalse() throws Exception {
        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(eventCaptor.capture(), listenerCaptor.capture())).thenReturn(mockSocket);

        publisher.afterPropertiesSet();
        publisher.connected = true;

        List<String> events = eventCaptor.getAllValues();
        List<Emitter.Listener> listeners = listenerCaptor.getAllValues();
        int idx = events.indexOf(Socket.EVENT_CONNECT_ERROR);
        listeners.get(idx).call();

        assertThat(publisher.isConnected()).isFalse();
    }
}
