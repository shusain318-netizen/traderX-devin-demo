package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeOrderTest {

    @Test
    void defaultConstructorCreatesInstance() {
        TradeOrder order = new TradeOrder();
        assertThat(order).isNotNull();
        assertThat(order.getId()).isNull();
        assertThat(order.getState()).isNull();
        assertThat(order.getSecurity()).isNull();
        assertThat(order.getQuantity()).isNull();
        assertThat(order.getAccountId()).isNull();
        assertThat(order.getSide()).isNull();
    }

    @Test
    void parameterizedConstructorSetsFields() {
        TradeOrder order = new TradeOrder("order-1", 42, "AAPL", TradeSide.Buy, 100);
        assertThat(order.getId()).isEqualTo("order-1");
        assertThat(order.getAccountId()).isEqualTo(42);
        assertThat(order.getSecurity()).isEqualTo("AAPL");
        assertThat(order.getSide()).isEqualTo(TradeSide.Buy);
        assertThat(order.getQuantity()).isEqualTo(100);
    }

    @Test
    void parameterizedConstructorWithSell() {
        TradeOrder order = new TradeOrder("order-2", 10, "MSFT", TradeSide.Sell, 50);
        assertThat(order.getSide()).isEqualTo(TradeSide.Sell);
        assertThat(order.getSecurity()).isEqualTo("MSFT");
    }
}
