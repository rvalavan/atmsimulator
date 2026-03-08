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

import java.math.BigDecimal;
import java.util.List;

/**
 * Sends a cash withdrawal request (txnCode 02) to the host.
 * Mirrors the existing single-operation withdrawal but in the reusable operation pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawOperation {

    private final NdcMessageBuilder messageBuilder;
    private final AtmHostGateway    hostGateway;

    public OperationResult execute(AtmSession session, BigDecimal amount) {
        log.info("[WITHDRAW] Requesting ${} for card ending {}", amount, last4(session.getCardNumber()));
        session.setState(TerminalState.TRANSACTION_PROCESSING);

        NdcMessage request = messageBuilder.buildTransactionRequest(
                session.getCardNumber(), session.getPin(),
                amount, session.getAccountType());
        NdcMessage response = hostGateway.sendTransaction(request);

        TransactionResult result = hostGateway.parseAuthorizationResponse(
                response, amount, session.getAccountType(), session.getCardNumber());

        List<NdcMessage> messages = List.of(request, response);
        session.addMessages(messages);

        if (result.isApproved()) {
            session.setState(TerminalState.DISPENSING);
            log.info("[WITHDRAW] Dispensing ${} — auth {}", result.getDispensedAmount(), result.getAuthorizationCode());
        } else {
            log.info("[WITHDRAW] DECLINED — {}", result.getHostMessage());
        }

        return OperationResult.builder()
                .operation("WITHDRAW")
                .success(result.isApproved())
                .status(result.isApproved() ? "APPROVED" : "DECLINED")
                .message(result.getHostMessage())
                .authorizationCode(result.getAuthorizationCode())
                .processedAmount(result.getDispensedAmount())
                .ndcMessages(messages)
                .build();
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4) : "****";
    }
}
