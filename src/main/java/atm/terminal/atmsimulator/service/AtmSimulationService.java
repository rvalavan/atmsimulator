package atm.terminal.atmsimulator.service;

import atm.terminal.atmsimulator.domain.TerminalState;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.model.request.AtmRequest;
import atm.terminal.atmsimulator.model.response.AtmResponse;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.service.gateway.AtmHostGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates a complete ATM withdrawal transaction.
 *
 * <p>The flow mirrors what a physical NCR ATM does:
 * <ol>
 *   <li>Terminal connects → sends Solicited Ready → receives host ack</li>
 *   <li>Card inserted → sends Track 2 data → receives "Send PIN" command</li>
 *   <li>PIN entered → sends transaction request with encrypted PIN block</li>
 *   <li>Host authorizes → terminal dispenses cash (or reports decline)</li>
 * </ol>
 *
 * <p>Every NDC message exchanged is captured in {@code ndcTrace} on the response
 * so test callers can inspect the full protocol exchange.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtmSimulationService {

    private final NdcMessageBuilder messageBuilder;
    private final AtmHostGateway    hostGateway;

    public AtmResponse processWithdrawal(AtmRequest request) {
        List<NdcMessage> trace = new ArrayList<>();
        TerminalState state = TerminalState.IDLE;

        try {
            // ── Step 1: Terminal connects, sends Solicited Ready ──────────────────
            log.info("[{}] Connecting to host — sending Solicited Ready", state);
            NdcMessage readyMsg = messageBuilder.buildSolicitedReady();
            trace.add(readyMsg);

            NdcMessage readyAck = hostGateway.connect(readyMsg);
            trace.add(readyAck);
            log.info("[{}] Host acknowledged terminal ready", state);

            // ── Step 2: Card inserted — send Track 2 data ────────────────────────
            state = TerminalState.CARD_READ;
            log.info("[{}] Card inserted — sending card data for PAN ending {}",
                    state, last4(request.getCardNumber()));

            NdcMessage cardMsg = messageBuilder.buildCardDataMessage(
                    request.getCardNumber(), request.getExpiryDate());
            trace.add(cardMsg);

            NdcMessage pinCmd = hostGateway.sendCardData(cardMsg);
            trace.add(pinCmd);
            log.info("[{}] Host sent PIN entry command: {}", state, pinCmd.readable());

            // ── Step 3: PIN entered — build PIN block, send transaction request ──
            state = TerminalState.PIN_ENTRY;
            log.info("[{}] PIN entered — sending withdrawal request for ${}", state, request.getAmount());

            NdcMessage txnRequest = messageBuilder.buildTransactionRequest(
                    request.getCardNumber(),
                    request.getPin(),
                    request.getAmount(),
                    request.getAccountType());
            trace.add(txnRequest);

            // ── Step 4: Host authorizes ───────────────────────────────────────────
            state = TerminalState.TRANSACTION_PROCESSING;
            NdcMessage authResponse = hostGateway.sendTransaction(txnRequest);
            trace.add(authResponse);

            TransactionResult result = hostGateway.parseAuthorizationResponse(
                    authResponse, request.getAmount(), request.getAccountType(), request.getCardNumber());

            // ── Step 5: Dispense cash / eject card ───────────────────────────────
            if (result.isApproved()) {
                state = TerminalState.DISPENSING;
                log.info("[{}] Dispensing ${} — auth code {}", state,
                        result.getDispensedAmount(), result.getAuthorizationCode());
            } else {
                log.info("[DECLINED] Host declined transaction: {}", result.getHostMessage());
            }

            state = TerminalState.CARD_EJECTED;
            log.info("[{}] Card ejected — transaction complete", state);

            return AtmResponse.builder()
                    .success(result.isApproved())
                    .status(result.isApproved() ? "APPROVED" : "DECLINED")
                    .authorizationCode(result.getAuthorizationCode())
                    .dispensedAmount(result.getDispensedAmount())
                    .message(result.getHostMessage())
                    .ndcTrace(trace)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Error during withdrawal simulation", state, e);
            return AtmResponse.builder()
                    .success(false)
                    .status("ERROR")
                    .message(e.getMessage())
                    .ndcTrace(trace)
                    .build();
        }
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4)
                : "****";
    }
}
