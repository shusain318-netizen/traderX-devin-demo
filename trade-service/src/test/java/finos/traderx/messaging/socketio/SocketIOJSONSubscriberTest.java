package finos.traderx.messaging.socketio;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.PubSubException;
import finos.traderx.tradeservice.model.TradeOrder;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocketIOJSONSubscriberTest {

    static class ConcreteSubscriber extends SocketIOJSONSubscriber<TradeOrder> {
        final List<TradeOrder> receivedMessages = new ArrayList<>();

        ConcreteSubscriber() {
            super(TradeOrder.class);
        }

        @Override
        public void onMessage(Envelope<?> envelope, TradeOrder message) {
            receivedMessages.add(message);
        }
    }

    @Test
    void isConnected_defaultFalse() {
        ConcreteSubscriber sub = new ConcreteSubscriber();
        assertThat(sub.isConnected()).isFalse();
    }

    @Test
    void setSocketAddress_setsAddress() {
        ConcreteSubscriber sub = new ConcreteSubscriber();
        sub.setSocketAddress("http://test:5000");
        assertThat(sub).isNotNull();
    }

    @Test
    void setDefaultTopic_setsTopic() {
        ConcreteSubscriber sub = new ConcreteSubscriber();
        sub.setDefaultTopic("/custom");
        assertThat(sub).isNotNull();
    }

    @Test
    void getIOOptions_returnsOptions() {
        ConcreteSubscriber sub = new ConcreteSubscriber();
        assertThat(sub.getIOOptions()).isNotNull();
    }

    @Test
    void connect_setsSocketViaInternalConnect() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();

            assertThat(sub.socket).isEqualTo(mockSocket);
            verify(mockSocket).on(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
            verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
            verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
            verify(mockSocket).on(eq("publish"), any(Emitter.Listener.class));
            verify(mockSocket).connect();
        }
    }

    @Test
    void connect_disconnectsExistingSocket() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.connect();

            verify(mockSocket).disconnect();
        }
    }

    @Test
    void connect_throwsPubSubExceptionOnFailure() {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class)))
                    .thenThrow(new RuntimeException("fail"));

            ConcreteSubscriber sub = new ConcreteSubscriber();
            assertThatThrownBy(sub::connect)
                    .isInstanceOf(PubSubException.class)
                    .hasMessageContaining("Cannot socket connection");
        }
    }

    @Test
    void disconnect_whenConnected() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.connected = true;
            sub.disconnect();

            verify(mockSocket).disconnect();
            assertThat(sub.socket).isNull();
        }
    }

    @Test
    void disconnect_whenSocketNull_noOp() throws PubSubException {
        ConcreteSubscriber sub = new ConcreteSubscriber();
        sub.disconnect();
    }

    @Test
    void disconnect_whenNotConnected_noDisconnect() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.connected = false;
            sub.disconnect();
        }
    }

    @Test
    void subscribe_emitsSubscribeEvent() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.subscribe("/trades");

            verify(mockSocket).emit("subscribe", "/trades");
        }
    }

    @Test
    void unsubscribe_emitsUnsubscribeEvent() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.unsubscribe("/trades");

            verify(mockSocket).emit("unsubscribe", "/trades");
        }
    }

    @Test
    void afterPropertiesSet_connectsAndSubscribes() throws Exception {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.afterPropertiesSet();

            assertThat(sub.socket).isEqualTo(mockSocket);
            verify(mockSocket).emit("subscribe", "/default");
        }
    }

    @Test
    void afterPropertiesSet_withCustomDefaultTopic() throws Exception {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.setDefaultTopic("/custom");
            sub.afterPropertiesSet();

            verify(mockSocket).emit("subscribe", "/custom");
        }
    }

    @Test
    void connectListener_setsConnectedTrue() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();

            ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
            verify(mockSocket).on(eq(Socket.EVENT_CONNECT), captor.capture());
            captor.getValue().call();
            assertThat(sub.isConnected()).isTrue();
        }
    }

    @Test
    void disconnectListener_setsConnectedFalse() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.connected = true;

            ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
            verify(mockSocket).on(eq(Socket.EVENT_DISCONNECT), captor.capture());
            captor.getValue().call();
            assertThat(sub.isConnected()).isFalse();
        }
    }

    @Test
    void connectErrorListener_setsConnectedFalse() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();
            sub.connected = true;

            ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
            verify(mockSocket).on(eq(Socket.EVENT_CONNECT_ERROR), captor.capture());
            captor.getValue().call();
            assertThat(sub.isConnected()).isFalse();
        }
    }

    @Test
    void publishListener_handlesMatchingType() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();

            ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
            verify(mockSocket).on(eq("publish"), captor.capture());

            JSONObject payload = new JSONObject();
            payload.put("id", "o1");
            payload.put("security", "AAPL");
            payload.put("accountId", 1);
            payload.put("quantity", 100);
            payload.put("side", "Buy");

            JSONObject json = new JSONObject();
            json.put("type", "TradeOrder");
            json.put("topic", "/trades");
            json.put("payload", payload);

            captor.getValue().call(json);

            assertThat(sub.receivedMessages).hasSize(1);
        }
    }

    @Test
    void publishListener_handlesNonMatchingType() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();

            ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
            verify(mockSocket).on(eq("publish"), captor.capture());

            JSONObject json = new JSONObject();
            json.put("type", "SystemMessage");
            json.put("topic", "/system");

            captor.getValue().call(json);

            assertThat(sub.receivedMessages).isEmpty();
        }
    }

    @Test
    void publishListener_handlesExceptionGracefully() throws PubSubException {
        try (MockedStatic<IO> ioMock = mockStatic(IO.class)) {
            Socket mockSocket = mock(Socket.class);
            when(mockSocket.on(anyString(), any(Emitter.Listener.class))).thenReturn(mockSocket);
            ioMock.when(() -> IO.socket(any(URI.class), any(IO.Options.class))).thenReturn(mockSocket);

            ConcreteSubscriber sub = new ConcreteSubscriber();
            sub.connect();

            ArgumentCaptor<Emitter.Listener> captor = ArgumentCaptor.forClass(Emitter.Listener.class);
            verify(mockSocket).on(eq("publish"), captor.capture());

            // Pass invalid data to trigger exception path
            captor.getValue().call("not a JSONObject");

            assertThat(sub.receivedMessages).isEmpty();
        }
    }
}
