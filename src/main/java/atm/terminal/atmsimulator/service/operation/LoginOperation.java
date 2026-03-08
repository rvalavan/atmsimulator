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
 * Handles the ATM login sequence:
 * <ol>
 *   <li>Terminal sends Solicited Ready → host acknowledges</li>
 *   <li>Terminal sends Track 2 card data → host sends "Enter PIN" command</li>
 * </ol>
 * After this operation the session state advances to {@link TerminalState#PIN_ENTRY}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginOperation {

    private final NdcMessageBuilder messageBuilder;
    private final AtmHostGateway    hostGateway;

    public OperationResult execute(AtmSession session) {
        log.info("[LOGIN] Starting for card ending {}", last4(session.getCardNumber()));

        // Step 1: Solicited Ready handshake
        NdcMessage ready = messageBuilder.buildSolicitedReady();
        NdcMessage readyAck = hostGateway.connect(ready);

        // Step 2: Card data → host sends Enter-PIN command
        NdcMessage cardData = messageBuilder.buildCardDataMessage(
                session.getCardNumber(), session.getExpiryDate());
        NdcMessage pinCmd = hostGateway.sendCardData(cardData);

        List<NdcMessage> messages = List.of(ready, readyAck, cardData, pinCmd);
        session.addMessages(messages);
        session.setState(TerminalState.PIN_ENTRY);

        log.info("[LOGIN] Card accepted — PIN entry requested");
        return OperationResult.builder()
                .operation("LOGIN")
                .success(true)
                .status("APPROVED")
                .message("Card accepted — PIN entry requested")
                .ndcMessages(messages)
                .build();
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4) : "****";
    }
}
