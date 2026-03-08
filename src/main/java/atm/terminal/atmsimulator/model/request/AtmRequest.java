package atm.terminal.atmsimulator.model.request;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.OperationType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * JSON payload accepted by the ATM simulator REST controller.
 *
 * Example — withdraw $100 from checking:
 * <pre>
 * {
 *   "cardNumber":   "4111111111111111",
 *   "expiryDate":   "2812",
 *   "pin":          "1234",
 *   "operation":    "WITHDRAW",
 *   "amount":       100.00,
 *   "accountType":  "CHECKING"
 * }
 * </pre>
 */
@Data
public class AtmRequest {

    /** 13–19 digit Primary Account Number (PAN) from the card. */
    private String cardNumber;

    /** Card expiry date in YYMM format (e.g. "2812" = December 2028). */
    private String expiryDate;

    /** Clear-text PIN entered by the cardholder (encrypted internally before sending). */
    private String pin;

    /** Operation to perform. */
    private OperationType operation;

    /** Transaction amount (used for WITHDRAW / DEPOSIT / TRANSFER). */
    private BigDecimal amount;

    /** Account type to debit / credit. */
    private AccountType accountType;
}
