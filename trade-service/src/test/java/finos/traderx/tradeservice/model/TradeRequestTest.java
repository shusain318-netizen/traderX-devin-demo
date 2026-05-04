package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeRequestTest {

    @Test
    void gettersAndSetters() {
        TradeRequest request = new TradeRequest();
        request.setAccountId(99);
        request.setSecurity("GOOG");
        request.setSide(TradeSide.Buy);
        request.setQuantity(200);

        assertThat(request.getAccountId()).isEqualTo(99);
        assertThat(request.getSecurity()).isEqualTo("GOOG");
        assertThat(request.getSide()).isEqualTo(TradeSide.Buy);
        assertThat(request.getQuantity()).isEqualTo(200);
    }

    @Test
    void sellSide() {
        TradeRequest request = new TradeRequest();
        request.setSide(TradeSide.Sell);
        assertThat(request.getSide()).isEqualTo(TradeSide.Sell);
    }
}
