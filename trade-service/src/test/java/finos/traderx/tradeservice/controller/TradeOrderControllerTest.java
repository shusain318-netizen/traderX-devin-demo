package finos.traderx.tradeservice.controller;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeOrderControllerTest {

    @Mock
    private Publisher<TradeOrder> tradePublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TradeOrderController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "referenceDataServiceAddress", "http://refdata:8080");
        ReflectionTestUtils.setField(controller, "accountServiceAddress", "http://accounts:8080");
    }

    @Test
    void createTradeOrder_validOrder_publishesAndReturnsOk() throws PubSubException {
        TradeOrder order = new TradeOrder("o1", 1, "AAPL", TradeSide.Buy, 100);

        when(restTemplate.getForEntity("http://refdata:8080//stocks/AAPL", Security.class))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple")));
        when(restTemplate.getForEntity("http://accounts:8080//account/1", Account.class))
                .thenReturn(ResponseEntity.ok(new Account(1, "Acc")));

        ResponseEntity<TradeOrder> result = controller.createTradeOrder(order);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(order);
        verify(tradePublisher).publish("/trades", order);
    }

    @Test
    void createTradeOrder_invalidTicker_throwsResourceNotFound() {
        TradeOrder order = new TradeOrder("o2", 1, "INVALID", TradeSide.Sell, 50);

        when(restTemplate.getForEntity("http://refdata:8080//stocks/INVALID", Security.class))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    void createTradeOrder_tickerValidation_nonNotFoundError() {
        TradeOrder order = new TradeOrder("o3", 1, "ERR", TradeSide.Buy, 10);

        when(restTemplate.getForEntity("http://refdata:8080//stocks/ERR", Security.class))
                .thenThrow(HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTradeOrder_invalidAccount_throwsResourceNotFound() {
        TradeOrder order = new TradeOrder("o4", 999, "AAPL", TradeSide.Buy, 10);

        when(restTemplate.getForEntity("http://refdata:8080//stocks/AAPL", Security.class))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple")));
        when(restTemplate.getForEntity("http://accounts:8080//account/999", Account.class))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void createTradeOrder_accountValidation_nonNotFoundError() {
        TradeOrder order = new TradeOrder("o5", 1, "AAPL", TradeSide.Buy, 10);

        when(restTemplate.getForEntity("http://refdata:8080//stocks/AAPL", Security.class))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple")));
        when(restTemplate.getForEntity("http://accounts:8080//account/1", Account.class))
                .thenThrow(HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null));

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTradeOrder_publishFails_throwsRuntimeException() throws PubSubException {
        TradeOrder order = new TradeOrder("o6", 1, "AAPL", TradeSide.Buy, 100);

        when(restTemplate.getForEntity("http://refdata:8080//stocks/AAPL", Security.class))
                .thenReturn(ResponseEntity.ok(new Security("AAPL", "Apple")));
        when(restTemplate.getForEntity("http://accounts:8080//account/1", Account.class))
                .thenReturn(ResponseEntity.ok(new Account(1, "Acc")));
        doThrow(new PubSubException("fail")).when(tradePublisher).publish("/trades", order);

        assertThatThrownBy(() -> controller.createTradeOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish");
    }
}
