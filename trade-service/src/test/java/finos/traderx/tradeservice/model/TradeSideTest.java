package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeSideTest {

    @Test
    void enumValues() {
        assertThat(TradeSide.values()).containsExactly(TradeSide.Buy, TradeSide.Sell);
    }

    @Test
    void valueOfBuy() {
        assertThat(TradeSide.valueOf("Buy")).isEqualTo(TradeSide.Buy);
    }

    @Test
    void valueOfSell() {
        assertThat(TradeSide.valueOf("Sell")).isEqualTo(TradeSide.Sell);
    }
}
