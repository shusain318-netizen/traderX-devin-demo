package finos.traderx.tradeservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocsControllerTest {

    private final DocsController controller = new DocsController();

    @Test
    void index_always_returnsRedirectToSwagger() {
        String result = controller.index();

        assertThat(result).isEqualTo("redirect:swagger-ui.html");
    }

    @Test
    void index_always_startsWithRedirectPrefix() {
        String result = controller.index();

        assertThat(result).startsWith("redirect:");
    }
}
