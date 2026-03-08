package atm.terminal.atmsimulator.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionResult {
    private boolean approved;
    private String  authorizationCode;
    private BigDecimal dispensedAmount;
    private String  hostMessage;

    /** Populated for BALANCE_INQUIRY operations — current account balance returned by host. */
    private BigDecimal currentBalance;
}
