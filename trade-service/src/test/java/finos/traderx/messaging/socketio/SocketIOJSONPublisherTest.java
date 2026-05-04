package finos.traderx.messaging.socketio;

import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocketIOJSONPublisherTest {

    @Mock
    private Socket mockSocket;

    private TestPublisher publisher;

    static class TestPublisher extends SocketIOJSONPublisher<TradeOrder> {
        private Socket socketOverride;

        void setSocketOverride(Socket s) {
            this.socketOverride = s;
        }

        @Override
        protected Socket internalConnect(URI uri) throws Exception {
            return socketOverride;
        }
    }

    @BeforeEach
    void setUp() {
        publisher = new TestPublisher();
        publisher.setSocketOverride(mockSocket);
    }

    @Test
    void isConnected_defaultFalse() {
        assertThat(publisher.isConnected()).isFalse();
    }

    @Test
    void setSocketAddress() {
        publisher.setSocketAddress("http://test:1234");
        assertThat(publisher).isNotNull();
    }

    @Test
    void setTopic() {
        publisher.setTopic("/custom");
        assertThat(publisher).isNotNull();
    }

    @Test
    void getIOOptions_returnsOptions() {
        assertThat(publisher.getIOOptions()).isNotNull();
    }

    @Test
    void connect_setsSocket() throws PubSubException {
        publisher.connect();
        assertThat(publisher.socket).isEqualTo(mockSocket);
    }

    @Test
    void connect_disconnectsExistingSocket() throws PubSubException {
        publisher.connect();
        Socket firstSocket = publisher.socket;
        publisher.connect();
        verify(firstSocket).disconnect();
    }

    @Test
    void connect_throwsPubSubExceptionOnFailure() {
        TestPublisher failPublisher = new TestPublisher() {
            @Override
            protected Socket internalConnect(URI uri) throws Exception {
                throw new RuntimeException("connection failed");
            }
        };
        assertThatThrownBy(failPublisher::connect)
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("Cannot socket connection");
    }

    @Test
    void disconnect_whenConnected() throws PubSubException {
        publisher.connect();
        publisher.connected = true;
        publisher.disconnect();
        verify(mockSocket).disconnect();
        assertThat(publisher.socket).isNull();
    }

    @Test
    void disconnect_whenSocketNull_noOp() throws PubSubException {
        publisher.disconnect();
    }

    @Test
    void disconnect_whenNotConnected_noDisconnect() throws PubSubException {
        publisher.connect();
        publisher.connected = false;
        publisher.disconnect();
        verify(mockSocket, never()).disconnect();
    }

    @Test
    void publishWithTopic_whenNotConnected_throwsPubSubException() {
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);
        assertThatThrownBy(() -> publisher.publish("/trades", order))
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("not connected");
    }

    @Test
    void publishWithDefaultTopic_whenNotConnected_throwsPubSubException() {
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);
        assertThatThrownBy(() -> publisher.publish(order))
                .isInstanceOf(PubSubException.class)
                .hasMessageContaining("not connected");
    }

    @Test
    void publishWithTopic_whenConnected_emitsToSocket() throws PubSubException {
        publisher.connect();
        publisher.connected = true;

        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);
        publisher.publish("/trades", order);

        verify(mockSocket).emit(eq("publish"), any(org.json.JSONObject.class));
    }

    @Test
    void publishWithDefaultTopic_whenConnected_emitsToSocket() throws PubSubException {
        publisher.connect();
        publisher.connected = true;
        publisher.setTopic("/my-topic");

        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);
        publisher.publish(order);

        verify(mockSocket).emit(eq("publish"), any(org.json.JSONObject.class));
    }

    @Test
    void publishWithTopic_whenEmitThrows_catchesException() throws PubSubException {
        publisher.connect();
        publisher.connected = true;
        when(mockSocket.emit(anyString(), any(org.json.JSONObject.class)))
                .thenThrow(new RuntimeException("emit failed"));

        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);
        publisher.publish("/trades", order);
        // exception is caught internally, no throw expected
    }

    @Test
    void afterPropertiesSet_connectsAndRegistersListeners() throws Exception {
        when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);

        publisher.afterPropertiesSet();

        verify(mockSocket).on(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
        verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
        verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
        verify(mockSocket).connect();
    }

    @Test
    void afterPropertiesSet_connectListenerSetsConnectedTrue() throws Exception {
        ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);

        publisher.afterPropertiesSet();

        verify(mockSocket).on(eq(Socket.EVENT_CONNECT), captor.capture());
        captor.getValue().call();
        assertThat(publisher.isConnected()).isTrue();
    }

    @Test
    void afterPropertiesSet_disconnectListenerSetsConnectedFalse() throws Exception {
        ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);

        publisher.afterPropertiesSet();
        publisher.connected = true;

        verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), captor.capture());
        captor.getValue().call();
        assertThat(publisher.isConnected()).isFalse();
    }

    @Test
    void afterPropertiesSet_connectErrorListenerSetsConnectedFalse() throws Exception {
        ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
        when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);

        publisher.afterPropertiesSet();
        publisher.connected = true;

        verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), captor.capture());
        captor.getValue().call();
        assertThat(publisher.isConnected()).isFalse();
    }

    @Test
    void realInternalConnect_callsIOSocket() throws Exception {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket realMock = mock(Socket.class);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(realMock);

            SocketIOJSONPublisher<TradeOrder> realPublisher = new SocketIOJSONPublisher<>() {};
            Socket result = realPublisher.internalConnect(URI.create("http://localhost:3000"));

            assertThat(result).isEqualTo(realMock);
        }
    }
}
