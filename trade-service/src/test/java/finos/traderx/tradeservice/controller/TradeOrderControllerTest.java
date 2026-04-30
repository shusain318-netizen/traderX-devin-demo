package finos.traderx.tradeservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

@ExtendWith(MockitoExtension.class)
class TradeOrderControllerTest {

    @Mock
    private Publisher<TradeOrder> tradePublisher;

    @Mock
    private RestTemplate restTemplate;

    private TradeOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new TradeOrderController();
        ReflectionTestUtils.setField(controller, "tradePublisher", tradePublisher);
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "referenceDataServiceAddress", "http://ref-data:8080");
        ReflectionTestUtils.setField(controller, "accountServiceAddress", "http://account-svc:8081");
    }

    private TradeOrder newOrder() {
        return new TradeOrder("order-1", 42, "AAPL", TradeSide.Buy, 100);
    }

    @Test
    void createTradeOrder_validTickerAndAccount_returnsOkWithOrder() throws PubSubException {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple Inc")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenReturn(ResponseEntity.ok(new Account(42, "Test Account")));

        ResponseEntity<TradeOrder> response = controller.createTradeOrder(order);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(order);
        verify(tradePublisher).publish("/trades", order);
    }

    @Test
    void createTradeOrder_invalidTicker_throwsResourceNotFoundException() {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AAPL");
    }

    @Test
    void createTradeOrder_tickerValidationNon404Error_throwsResourceNotFoundException() {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AAPL");
    }

    @Test
    void createTradeOrder_invalidAccount_throwsResourceNotFoundException() {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple Inc")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void createTradeOrder_accountValidationNon404Error_throwsResourceNotFoundException() {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple Inc")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void createTradeOrder_publishFails_throwsRuntimeException() throws PubSubException {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple Inc")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenReturn(ResponseEntity.ok(new Account(42, "Test Account")));
        doThrow(new PubSubException("connection lost")).when(tradePublisher).publish(anyString(), any());

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish trade order")
                .hasCauseInstanceOf(PubSubException.class);
    }

    @Test
    void createTradeOrder_validOrder_callsCorrectTickerValidationUrl() throws PubSubException {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple Inc")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenReturn(ResponseEntity.ok(new Account(42, "Test Account")));

        controller.createTradeOrder(order);

        verify(restTemplate).getForEntity("http://ref-data:8080//stocks/AAPL", Security.class);
    }

    @Test
    void createTradeOrder_validOrder_callsCorrectAccountValidationUrl() throws PubSubException {
        TradeOrder order = newOrder();
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple Inc")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenReturn(ResponseEntity.ok(new Account(42, "Test Account")));

        controller.createTradeOrder(order);

        verify(restTemplate).getForEntity("http://account-svc:8081//account/42", Account.class);
    }

    @Test
    void createTradeOrder_sellOrder_handledCorrectly() throws PubSubException {
        TradeOrder order = new TradeOrder("order-2", 10, "MSFT", TradeSide.Sell, 50);
        when(restTemplate.getForEntity(anyString(), eq(Security.class)))
                .thenReturn(ResponseEntity.ok(new Security("MSFT", "Microsoft")));
        when(restTemplate.getForEntity(anyString(), eq(Account.class)))
                .thenReturn(ResponseEntity.ok(new Account(10, "Sell Account")));

        ResponseEntity<TradeOrder> response = controller.createTradeOrder(order);

        assertThat(response.getBody().getSecurity()).isEqualTo("MSFT");
        assertThat(response.getBody().getSide()).isEqualTo(TradeSide.Sell);
        verify(tradePublisher).publish("/trades", order);
    }
}
