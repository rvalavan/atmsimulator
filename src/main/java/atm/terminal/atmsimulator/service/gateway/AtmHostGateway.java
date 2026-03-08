package atm.terminal.atmsimulator.service.gateway;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.protocol.NdcMessage;

import java.math.BigDecimal;

/**
 * Abstraction over the NCR ATM host communication channel.
 *
 * Two implementations are provided:
 * <ul>
 *   <li>{@link SimulatedHostGateway} — in-memory simulation, no network required
 *       (active when {@code atm.host.simulated=true}, which is the default)</li>
 *   <li>{@link RealNdcHostGateway} — live TCP/IP connection to an NCR host
 *       (active when {@code atm.host.simulated=false})</li>
 * </ul>
 */
public interface AtmHostGateway {

    /**
     * Sends the terminal Solicited Ready message and returns the host acknowledgement.
     */
    NdcMessage connect(NdcMessage readyMessage);

    /**
     * Sends Track 2 card data to the host and returns the host's next command
     * (typically a "Send PIN" state transition).
     */
    NdcMessage sendCardData(NdcMessage cardDataMessage);

    /**
     * Sends the transaction request (PIN block + amount) to the host
     * and returns the authorization response message.
     */
    NdcMessage sendTransaction(NdcMessage transactionMessage);

    /**
     * Parses the host authorization response into a structured result.
     */
    TransactionResult parseAuthorizationResponse(NdcMessage hostResponse,
                                                 BigDecimal requestedAmount,
                                                 AccountType accountType);
}
