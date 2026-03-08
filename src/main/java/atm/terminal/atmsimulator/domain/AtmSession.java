package atm.terminal.atmsimulator.domain;

import atm.terminal.atmsimulator.protocol.NdcMessage;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries shared state across all operations within a single ATM session.
 * Passed into each operation class; operations append their NDC messages
 * to the trace and advance the terminal state.
 */
@Data
@Builder
public class AtmSession {

    private String cardNumber;
    private String expiryDate;
    private String pin;
    private AccountType accountType;

    @Builder.Default
    private TerminalState state = TerminalState.IDLE;

    @Builder.Default
    private List<NdcMessage> ndcTrace = new ArrayList<>();

    public void addMessage(NdcMessage message) {
        ndcTrace.add(message);
    }

    public void addMessages(List<NdcMessage> messages) {
        ndcTrace.addAll(messages);
    }
}
