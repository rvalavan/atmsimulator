package atm.terminal.atmsimulator.service.operation;

import atm.terminal.atmsimulator.domain.AtmSession;
import atm.terminal.atmsimulator.domain.TerminalState;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.service.NdcMessageBuilder;
import atm.terminal.atmsimulator.service.gateway.AtmHostGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ejects the card and notifies the host that the session has ended.
 * Sends UNSOLICITED sub-class 'B' (card ejected) and advances the session
 * state back to {@link TerminalState#IDLE}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutOperation {

    private final NdcMessageBuilder messageBuilder;
    private final AtmHostGateway    hostGateway;

    public OperationResult execute(AtmSession session) {
        log.info("[LOGOUT] Ejecting card ending {}", last4(session.getCardNumber()));

        NdcMessage logout = messageBuilder.buildLogout();
        NdcMessage ack    = hostGateway.sendMessage(logout);

        List<NdcMessage> messages = List.of(logout, ack);
        session.addMessages(messages);
        session.setState(TerminalState.IDLE);

        log.info("[LOGOUT] Session ended — terminal returned to IDLE");
        return OperationResult.builder()
                .operation("LOGOUT")
                .success(true)
                .status("APPROVED")
                .message("Card ejected — session ended")
                .ndcMessages(messages)
                .build();
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4) : "****";
    }
}
