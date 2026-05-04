package finos.traderx.tradeservice.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocsControllerTest {

    @Test
    void indexRedirectsToSwagger() {
        DocsController controller = new DocsController();
        String result = controller.index();
        assertThat(result).isEqualTo("redirect:swagger-ui.html");
    }
}
