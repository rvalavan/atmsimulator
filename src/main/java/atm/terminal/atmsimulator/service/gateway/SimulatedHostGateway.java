package atm.terminal.atmsimulator.service.gateway;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.protocol.NdcDelimiter;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * In-process simulation of the NCR ATM host.
 * Active when {@code atm.host.simulated=true} (default).
 *
 * Approval rules:
 * <ul>
 *   <li>Amount &le; $500 → APPROVED</li>
 *   <li>Amount &gt; $500 → DECLINED (exceeds limit)</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "atm.host.simulated", havingValue = "true", matchIfMissing = true)
public class SimulatedHostGateway implements AtmHostGateway {

    private static final BigDecimal APPROVAL_LIMIT = BigDecimal.valueOf(500);

    @Override
    public NdcMessage connect(NdcMessage readyMessage) {
        log.debug("SIM HOST received: {}", readyMessage.readable());

        // Host acknowledges the terminal is in-service
        String raw = "2" + NdcDelimiter.FS + "F"
                + NdcDelimiter.FS + "00000000"
                + NdcDelimiter.FS + "0";

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.SOLICITED)
                .messageSubClass("F")
                .direction("HOST->TERMINAL")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public NdcMessage sendCardData(NdcMessage cardDataMessage) {
        log.debug("SIM HOST received card data: {}", cardDataMessage.readable());

        // Host transitions terminal to state 024 = "Enter PIN" screen
        String raw = "3" + NdcDelimiter.FS + "8"
                + NdcDelimiter.FS + "024";

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.HOST_COMMAND)
                .messageSubClass("8")
                .direction("HOST->TERMINAL")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public NdcMessage sendTransaction(NdcMessage transactionMessage) {
        log.debug("SIM HOST received transaction: {}", transactionMessage.readable());

        // Extract amount from the raw message to decide approval
        // The status code is embedded in the response; actual parse happens in parseAuthorizationResponse
        String raw = "4" + NdcDelimiter.FS + "PENDING";

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.HOST_DATA)
                .messageSubClass("A")
                .direction("HOST->TERMINAL")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public TransactionResult parseAuthorizationResponse(NdcMessage hostResponse,
                                                        BigDecimal requestedAmount,
                                                        AccountType accountType) {
        boolean approved = requestedAmount.compareTo(APPROVAL_LIMIT) <= 0;

        if (approved) {
            String authCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            log.info("SIM HOST: APPROVED {} {} - auth code {}", requestedAmount, accountType, authCode);
            return TransactionResult.builder()
                    .approved(true)
                    .authorizationCode(authCode)
                    .dispensedAmount(requestedAmount)
                    .hostMessage("APPROVED")
                    .build();
        }

        log.info("SIM HOST: DECLINED {} {} - exceeds limit {}", requestedAmount, accountType, APPROVAL_LIMIT);
        return TransactionResult.builder()
                .approved(false)
                .dispensedAmount(BigDecimal.ZERO)
                .hostMessage("DECLINED - EXCEEDS LIMIT")
                .build();
    }
}
