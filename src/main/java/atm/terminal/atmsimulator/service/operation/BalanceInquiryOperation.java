package atm.terminal.atmsimulator.service.operation;

import atm.terminal.atmsimulator.domain.AtmSession;
import atm.terminal.atmsimulator.domain.TerminalState;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.service.NdcMessageBuilder;
import atm.terminal.atmsimulator.service.gateway.AtmHostGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sends a balance inquiry (txnCode 01) to the host and returns the current balance.
 * Can be reused at any point in a session where PIN has already been entered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceInquiryOperation {

    private final NdcMessageBuilder messageBuilder;
    private final AtmHostGateway    hostGateway;

    public OperationResult execute(AtmSession session) {
        log.info("[BALANCE] Requesting balance for card ending {}", last4(session.getCardNumber()));
        session.setState(TerminalState.TRANSACTION_PROCESSING);

        NdcMessage request = messageBuilder.buildBalanceInquiry(
                session.getCardNumber(), session.getPin(), session.getAccountType());
        NdcMessage response = hostGateway.sendTransaction(request);

        TransactionResult result = hostGateway.parseBalanceResponse(response, session.getCardNumber());

        List<NdcMessage> messages = List.of(request, response);
        session.addMessages(messages);

        log.info("[BALANCE] Current balance: {}", result.getCurrentBalance());
        return OperationResult.builder()
                .operation("BALANCE")
                .success(result.isApproved())
                .status(result.isApproved() ? "APPROVED" : "ERROR")
                .message(result.getHostMessage())
                .balance(result.getCurrentBalance())
                .ndcMessages(messages)
                .build();
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4) : "****";
    }
}
