package atm.terminal.atmsimulator.model.response;

import atm.terminal.atmsimulator.protocol.NdcMessage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Top-level response for a multi-step ATM scenario.
 * Contains the result of each individual operation in order,
 * plus the combined NDC message trace across the whole session.
 */
@Data
@Builder
public class ScenarioResponse {

    /** Name of the scenario executed. */
    private String scenario;

    /** true only if every operation in the scenario succeeded. */
    private boolean success;

    /** Ordered results of each operation step (login, balance, transfer, etc.). */
    private List<OperationResult> operations;

    /** All NDC messages exchanged during the entire session, in chronological order. */
    private List<NdcMessage> fullNdcTrace;
}
