package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeStateTest {

    @Test
    void enumValues() {
        assertThat(TradeState.values()).containsExactly(
                TradeState.New, TradeState.Processing, TradeState.Settled, TradeState.Cancelled);
    }

    @Test
    void valueOf() {
        assertThat(TradeState.valueOf("New")).isEqualTo(TradeState.New);
        assertThat(TradeState.valueOf("Processing")).isEqualTo(TradeState.Processing);
        assertThat(TradeState.valueOf("Settled")).isEqualTo(TradeState.Settled);
        assertThat(TradeState.valueOf("Cancelled")).isEqualTo(TradeState.Cancelled);
    }
}
