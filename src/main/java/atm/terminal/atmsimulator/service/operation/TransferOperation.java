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
 * Sends a fund transfer request (txnCode 03) to the host.
 * The simulated host deducts the transferred amount from the cardholder's balance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferOperation {

    private final NdcMessageBuilder messageBuilder;
    private final AtmHostGateway    hostGateway;

    public OperationResult execute(AtmSession session, String toAccountNumber, BigDecimal amount) {
        log.info("[TRANSFER] ${} from card ending {} to account {}",
                amount, last4(session.getCardNumber()), toAccountNumber);
        session.setState(TerminalState.TRANSACTION_PROCESSING);

        NdcMessage request = messageBuilder.buildTransferRequest(
                session.getCardNumber(), session.getPin(),
                amount, session.getAccountType(), toAccountNumber);
        NdcMessage response = hostGateway.sendTransaction(request);

        TransactionResult result = hostGateway.parseAuthorizationResponse(
                response, amount, session.getAccountType(), session.getCardNumber());

        List<NdcMessage> messages = List.of(request, response);
        session.addMessages(messages);

        log.info("[TRANSFER] Result: {} — auth {}", result.isApproved() ? "APPROVED" : "DECLINED",
                result.getAuthorizationCode());
        return OperationResult.builder()
                .operation("TRANSFER")
                .success(result.isApproved())
                .status(result.isApproved() ? "APPROVED" : "DECLINED")
                .message(result.getHostMessage())
                .authorizationCode(result.getAuthorizationCode())
                .processedAmount(result.isApproved() ? amount : BigDecimal.ZERO)
                .ndcMessages(messages)
                .build();
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4) : "****";
    }
}
