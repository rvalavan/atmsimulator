package atm.terminal.atmsimulator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTypeTest {

    @Test
    void ndcCode_checking_returns10() {
        assertEquals("10", AccountType.CHECKING.ndcCode());
    }

    @Test
    void ndcCode_savings_returns20() {
        assertEquals("20", AccountType.SAVINGS.ndcCode());
    }

    @Test
    void ndcCode_credit_returns30() {
        assertEquals("30", AccountType.CREDIT.ndcCode());
    }

    @Test
    void ndcCode_allTypes_areTwoDigits() {
        for (AccountType type : AccountType.values()) {
            assertEquals(2, type.ndcCode().length(),
                    type.name() + " ndcCode should be exactly 2 digits");
        }
    }

    @Test
    void allThreeAccountTypes_exist() {
        assertEquals(3, AccountType.values().length);
    }
}
