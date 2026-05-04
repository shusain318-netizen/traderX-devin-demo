package finos.traderx.tradeservice;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void configReturnsValidOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "port", 18092);

        OpenAPI api = config.config();

        assertThat(api).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("FINOS TraderX Trading Service");
        assertThat(api.getInfo().getVersion()).isEqualTo("0.1.0");
        assertThat(api.getInfo().getDescription()).contains("capturing trades");
        assertThat(api.getServers()).hasSize(2);
        assertThat(api.getServers().get(0).getUrl()).isEmpty();
        assertThat(api.getServers().get(1).getUrl()).isEqualTo("http://localhost:18092");
    }
}
