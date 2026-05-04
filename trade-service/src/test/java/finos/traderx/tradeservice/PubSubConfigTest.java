package finos.traderx.tradeservice;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PubSubConfigTest {

    @Test
    void tradePublisherBeanIsCreated() {
        PubSubConfig config = new PubSubConfig();
        ReflectionTestUtils.setField(config, "tradeFeedAddress", "http://localhost:9999");

        Publisher<TradeOrder> publisher = config.tradePublisher();

        assertThat(publisher).isNotNull();
    }
}
