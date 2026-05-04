package finos.traderx.tradeservice.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void constructorSetsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        assertThat(ex.getMessage()).isEqualTo("not found");
    }

    @Test
    void isRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
