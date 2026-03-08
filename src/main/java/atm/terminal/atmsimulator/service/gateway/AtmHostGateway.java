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
     * Sends a transaction request (balance inquiry, transfer, or withdrawal)
     * and returns the host authorization response message.
     */
    NdcMessage sendTransaction(NdcMessage transactionMessage);

    /**
     * Generic send — used for logout / card-ejected notifications and
     * any message that does not fit the more specific methods above.
     */
    NdcMessage sendMessage(NdcMessage message);

    /**
     * Parses the host authorization response (for withdrawal and transfer) into
     * a structured result. On approval, deducts the amount from the cardholder's
     * simulated balance (simulation mode only).
     *
     * @param cardNumber used by the simulated gateway for balance tracking
     */
    TransactionResult parseAuthorizationResponse(NdcMessage hostResponse,
                                                 BigDecimal requestedAmount,
                                                 AccountType accountType,
                                                 String cardNumber);

    /**
     * Parses the host response to a balance inquiry and returns the current balance
     * in {@link TransactionResult#getCurrentBalance()}.
     *
     * @param cardNumber used by the simulated gateway to look up the in-memory balance
     */
    TransactionResult parseBalanceResponse(NdcMessage hostResponse, String cardNumber);
}
