package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityTest {

    @Test
    void defaultConstructor() {
        Security security = new Security();
        assertThat(security).isNotNull();
        assertThat(security.getTicker()).isNull();
        assertThat(security.getcompanyName()).isNull();
    }

    @Test
    void parameterizedConstructor() {
        Security security = new Security("AAPL", "Apple Inc.");
        assertThat(security.getTicker()).isEqualTo("AAPL");
        assertThat(security.getcompanyName()).isEqualTo("Apple Inc.");
    }
}
