package finos.traderx.tradeservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class TradeServiceApplicationTest {

    @Test
    void mainCallsSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(TradeServiceApplication.class, new String[]{}))
                    .thenReturn(null);

            TradeServiceApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(TradeServiceApplication.class, new String[]{}));
        }
    }
}
