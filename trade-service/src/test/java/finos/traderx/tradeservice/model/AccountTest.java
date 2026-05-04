package finos.traderx.tradeservice.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    void defaultConstructor() {
        Account account = new Account();
        assertThat(account).isNotNull();
        assertThat(account.getid()).isNull();
        assertThat(account.getdisplayName()).isNull();
    }

    @Test
    void parameterizedConstructor() {
        Account account = new Account(1, "Test Account");
        assertThat(account.getid()).isEqualTo(1);
        assertThat(account.getdisplayName()).isEqualTo("Test Account");
    }
}
