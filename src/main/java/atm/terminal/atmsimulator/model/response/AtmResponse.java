package atm.terminal.atmsimulator.model.response;

import atm.terminal.atmsimulator.protocol.NdcMessage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response returned by the ATM simulator after processing an operation.
 * Includes the full NDC message trace so callers can inspect the exact
 * messages exchanged with the host — useful for protocol-level testing.
 */
@Data
@Builder
public class AtmResponse {

    /** true if the transaction was approved by the host. */
    private boolean success;

    /** Host decision: APPROVED, DECLINED, or ERROR. */
    private String status;

    /** 6-character authorization code returned on approval. */
    private String authorizationCode;

    /** Amount actually dispensed (0 if declined). */
    private BigDecimal dispensedAmount;

    /** Human-readable message from the host or error description. */
    private String message;

    /** Ordered list of every NDC message sent and received during this transaction. */
    private List<NdcMessage> ndcTrace;
}
