package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeResponseTest {

    @Test
    void gettersAndSetters() {
        TradeResponse response = new TradeResponse();
        response.setId("resp-1");
        response.setSuccess(true);
        response.setErrorMessage("some error");

        assertThat(response.getId()).isEqualTo("resp-1");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getErrorMessage()).isEqualTo("some error");
    }

    @Test
    void successFactoryMethod() {
        TradeResponse response = TradeResponse.success("abc-123");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getId()).isEqualTo("abc-123");
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void errorFactoryMethod() {
        TradeResponse response = TradeResponse.error("failed");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("failed");
        assertThat(response.getId()).isNull();
    }
}
