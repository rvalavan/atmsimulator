package atm.terminal.atmsimulator.model.response;

import atm.terminal.atmsimulator.protocol.NdcMessage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a single atomic ATM operation (login, balance, transfer, withdraw, logout).
 * Scenario services accumulate these into a {@link ScenarioResponse}.
 */
@Data
@Builder
public class OperationResult {

    /** Which operation produced this result: LOGIN, BALANCE, TRANSFER, WITHDRAW, LOGOUT. */
    private String operation;

    /** Whether the operation succeeded. */
    private boolean success;

    /** APPROVED, DECLINED, or ERROR. */
    private String status;

    /** Human-readable message from host or error description. */
    private String message;

    /** Populated for BALANCE operations — current account balance returned by host. */
    private BigDecimal balance;

    /** Populated for TRANSFER and WITHDRAW operations. */
    private String authorizationCode;

    /** Amount processed (transferred or dispensed). Zero if declined. */
    private BigDecimal processedAmount;

    /** NDC messages exchanged during this specific operation. */
    private List<NdcMessage> ndcMessages;
}
