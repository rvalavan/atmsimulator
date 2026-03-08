package atm.terminal.atmsimulator.service;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.protocol.NdcDelimiter;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import atm.terminal.atmsimulator.protocol.PinBlockUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Builds outbound NDC messages (TERMINAL → HOST direction).
 * All messages follow the NCR NDC protocol framing conventions.
 */
@Component
public class NdcMessageBuilder {

    @Value("${atm.terminal.id:00000001}")
    private String terminalId;

    @Value("${atm.institution.id:0001}")
    private String institutionId;

    /**
     * Solicited Ready — sent by the terminal after establishing a connection
     * to inform the host the terminal is in-service and waiting for a card.
     *
     * Format: 2 FS F FS <terminalId> FS 0 FS 000
     *   Class 2 = Solicited, Sub-class F = Ready
     *   Error severity 0, error code 000 = no error
     */
    public NdcMessage buildSolicitedReady() {
        String raw = "2" + NdcDelimiter.FS + "F"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + "0"
                + NdcDelimiter.FS + "000";

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.SOLICITED)
                .messageSubClass("F")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Card Data message — sent after card insertion with Track 2 data.
     * The host uses this to identify the cardholder and determine the next state.
     *
     * Track 2 format: ; PAN = YYMM ServiceCode ?
     * Minimal Track 2 for ATM: ; PAN = YYMM ?
     */
    public NdcMessage buildCardDataMessage(String cardNumber, String expiryDate) {
        String track2 = ";" + cardNumber + "=" + expiryDate + "?";

        String raw = "1" + NdcDelimiter.FS + "E"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + institutionId
                + NdcDelimiter.GS + track2;

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("E")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Transaction Request — sent after PIN entry carrying the PIN block, PAN,
     * account type, and requested amount to the host.
     *
     * Sub-class T = Transaction Request (NDC standard).
     *
     * Frame layout (all post-header fields separated by GS):
     * <pre>
     *   1 FS T FS terminalId FS institutionId
     *     GS txnCode     — 6 chars: 2-digit op (02=withdrawal) + 4-digit flags (0000)
     *     GS amount      — 12 digits zero-padded cents (e.g. $100.00 → 000000010000)
     *     GS accountType — 2 digits: from-account + to-account (e.g. 10 = CHECKING/none)
     *     GS cardNumber  — PAN
     *     GS pinBlock    — ISO 9564 Format 0, 16 hex digits
     * </pre>
     *
     * PIN block is clear-text for simulation. In production it must be encrypted
     * under the Terminal Working Key (TWK) using 3DES before transmission.
     */
    public NdcMessage buildTransactionRequest(String cardNumber, String pin,
                                              BigDecimal amount, AccountType accountType) {
        String pinBlock  = PinBlockUtil.buildPinBlock(pin, cardNumber);
        String amountStr = String.format("%012d", amount.movePointRight(2).longValue());

        String raw = "1" + NdcDelimiter.FS + "T"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + institutionId
                + NdcDelimiter.GS + "020000"              // 02 = withdrawal, 0000 = flags
                + NdcDelimiter.GS + amountStr
                + NdcDelimiter.GS + accountType.ndcCode()
                + NdcDelimiter.GS + cardNumber
                + NdcDelimiter.GS + pinBlock;

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("T")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Balance Inquiry Request — txnCode 01, zero amount.
     *
     * <pre>
     *   1 FS T FS terminalId FS institutionId
     *     GS 010000            — 01 = balance inquiry
     *     GS 000000000000      — zero amount
     *     GS accountType
     *     GS cardNumber
     *     GS pinBlock
     * </pre>
     */
    public NdcMessage buildBalanceInquiry(String cardNumber, String pin, AccountType accountType) {
        String pinBlock = PinBlockUtil.buildPinBlock(pin, cardNumber);

        String raw = "1" + NdcDelimiter.FS + "T"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + institutionId
                + NdcDelimiter.GS + "010000"              // 01 = balance inquiry
                + NdcDelimiter.GS + "000000000000"        // zero amount
                + NdcDelimiter.GS + accountType.ndcCode()
                + NdcDelimiter.GS + cardNumber
                + NdcDelimiter.GS + pinBlock;

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("T")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Fund Transfer Request — txnCode 03, includes destination account number.
     *
     * <pre>
     *   1 FS T FS terminalId FS institutionId
     *     GS 030000            — 03 = transfer
     *     GS amount12
     *     GS accountType
     *     GS toAccountNumber
     *     GS cardNumber
     *     GS pinBlock
     * </pre>
     */
    public NdcMessage buildTransferRequest(String cardNumber, String pin,
                                           BigDecimal amount, AccountType accountType,
                                           String toAccountNumber) {
        String pinBlock  = PinBlockUtil.buildPinBlock(pin, cardNumber);
        String amountStr = String.format("%012d", amount.movePointRight(2).longValue());

        String raw = "1" + NdcDelimiter.FS + "T"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + institutionId
                + NdcDelimiter.GS + "030000"              // 03 = transfer
                + NdcDelimiter.GS + amountStr
                + NdcDelimiter.GS + accountType.ndcCode()
                + NdcDelimiter.GS + toAccountNumber
                + NdcDelimiter.GS + cardNumber
                + NdcDelimiter.GS + pinBlock;

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("T")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Logout / card-ejected notification — signals end of session.
     * The terminal returns to idle and notifies the host with UNSOLICITED sub-class 'B'.
     *
     * <pre>
     *   1 FS B FS terminalId FS institutionId GS CARD_EJECTED
     * </pre>
     */
    public NdcMessage buildLogout() {
        String raw = "1" + NdcDelimiter.FS + "B"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + institutionId
                + NdcDelimiter.GS + "CARD_EJECTED";

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("B")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }
}
