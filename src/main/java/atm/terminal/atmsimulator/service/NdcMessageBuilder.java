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
     * Transaction Request — sent after PIN entry with the encrypted PIN block,
     * account type, and requested amount.
     *
     * Transaction code 001 = Withdrawal.
     * Amount is expressed in cents, zero-padded to 12 digits.
     * PIN block is ISO 9564 Format 0 (clear-text for simulation).
     */
    public NdcMessage buildTransactionRequest(String cardNumber, String pin,
                                              BigDecimal amount, AccountType accountType) {
        String pinBlock  = PinBlockUtil.buildPinBlock(pin, cardNumber);
        String amountStr = String.format("%012d", amount.movePointRight(2).longValue());

        String raw = "1" + NdcDelimiter.FS + "E"
                + NdcDelimiter.FS + terminalId
                + NdcDelimiter.FS + institutionId
                + NdcDelimiter.FS + "001"                     // transaction code: withdrawal
                + NdcDelimiter.GS + pinBlock
                + NdcDelimiter.GS + accountType.ndcCode()
                + NdcDelimiter.GS + amountStr;

        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("E")
                .direction("TERMINAL->HOST")
                .rawMessage(raw)
                .timestamp(Instant.now())
                .build();
    }
}
