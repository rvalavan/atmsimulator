package atm.terminal.atmsimulator.model.request;

import atm.terminal.atmsimulator.domain.AccountType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * JSON input for multi-step scenario endpoints.
 * All card/PIN fields are required. Transfer and withdrawal
 * fields are only required for scenarios that include those steps.
 *
 * <h3>Scenario 1 — Balance check</h3>
 * <pre>
 * { "cardNumber": "4111111111111111", "expiryDate": "2812",
 *   "pin": "1234", "accountType": "CHECKING" }
 * </pre>
 *
 * <h3>Scenario 2 — Transfer then balance</h3>
 * <pre>
 * { "cardNumber": "4111111111111111", "expiryDate": "2812",
 *   "pin": "1234", "accountType": "CHECKING",
 *   "toAccountNumber": "9876543210", "transferAmount": 50.00 }
 * </pre>
 *
 * <h3>Scenario 3 — Transfer, withdraw, balance</h3>
 * <pre>
 * { "cardNumber": "4111111111111111", "expiryDate": "2812",
 *   "pin": "1234", "accountType": "CHECKING",
 *   "toAccountNumber": "9876543210", "transferAmount": 50.00,
 *   "withdrawAmount": 100.00 }
 * </pre>
 */
@Data
public class ScenarioRequest {

    // ── Card identity (required for all scenarios) ─────────────────────────
    private String cardNumber;
    private String expiryDate;
    private String pin;
    private AccountType accountType;

    // ── Transfer fields (required for scenarios 2 and 3) ───────────────────
    private String toAccountNumber;
    private BigDecimal transferAmount;

    // ── Withdrawal field (required for scenario 3) ─────────────────────────
    private BigDecimal withdrawAmount;
}
