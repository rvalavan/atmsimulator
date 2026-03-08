package atm.terminal.atmsimulator.service;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.protocol.NdcDelimiter;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NdcMessageBuilderTest {

    private NdcMessageBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new NdcMessageBuilder();
        ReflectionTestUtils.setField(builder, "terminalId", "00000001");
        ReflectionTestUtils.setField(builder, "institutionId", "0001");
    }

    // ── buildSolicitedReady ──────────────────────────────────────────────────

    @Test
    void buildSolicitedReady_messageClass_isSolicited() {
        NdcMessage msg = builder.buildSolicitedReady();
        assertEquals(NdcMessageClass.SOLICITED, msg.getMessageClass());
    }

    @Test
    void buildSolicitedReady_subClass_isF() {
        assertEquals("F", builder.buildSolicitedReady().getMessageSubClass());
    }

    @Test
    void buildSolicitedReady_direction_isTerminalToHost() {
        assertEquals("TERMINAL->HOST", builder.buildSolicitedReady().getDirection());
    }

    @Test
    void buildSolicitedReady_rawMessage_containsTerminalId() {
        String raw = builder.buildSolicitedReady().getRawMessage();
        assertTrue(raw.contains("00000001"));
    }

    @Test
    void buildSolicitedReady_rawMessage_startsWithClass2() {
        assertEquals("2", builder.buildSolicitedReady().getRawMessage().substring(0, 1));
    }

    @Test
    void buildSolicitedReady_timestamp_isNotNull() {
        assertNotNull(builder.buildSolicitedReady().getTimestamp());
    }

    // ── buildCardDataMessage ─────────────────────────────────────────────────

    @Test
    void buildCardDataMessage_messageClass_isUnsolicited() {
        NdcMessage msg = builder.buildCardDataMessage("4111111111111111", "2812");
        assertEquals(NdcMessageClass.UNSOLICITED, msg.getMessageClass());
    }

    @Test
    void buildCardDataMessage_subClass_isE() {
        assertEquals("E", builder.buildCardDataMessage("4111111111111111", "2812").getMessageSubClass());
    }

    @Test
    void buildCardDataMessage_rawMessage_containsTrack2Format() {
        String raw = builder.buildCardDataMessage("4111111111111111", "2812").getRawMessage();
        assertTrue(raw.contains(";4111111111111111=2812?"));
    }

    @Test
    void buildCardDataMessage_rawMessage_containsInstitutionId() {
        String raw = builder.buildCardDataMessage("4111111111111111", "2812").getRawMessage();
        assertTrue(raw.contains("0001"));
    }

    // ── buildTransactionRequest ──────────────────────────────────────────────

    @Test
    void buildTransactionRequest_messageClass_isUnsolicited() {
        NdcMessage msg = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(100), AccountType.CHECKING);
        assertEquals(NdcMessageClass.UNSOLICITED, msg.getMessageClass());
    }

    @Test
    void buildTransactionRequest_subClass_isT() {
        NdcMessage msg = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(100), AccountType.CHECKING);
        assertEquals("T", msg.getMessageSubClass());
    }

    @Test
    void buildTransactionRequest_rawMessage_containsWithdrawalTxnCode() {
        String raw = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(100), AccountType.CHECKING).getRawMessage();
        assertTrue(raw.contains("020000"));
    }

    @Test
    void buildTransactionRequest_rawMessage_containsAmountIn12DigitCents() {
        // $100 → 12-digit cents = 000000010000
        String raw = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(100), AccountType.CHECKING).getRawMessage();
        assertTrue(raw.contains("000000010000"));
    }

    @Test
    void buildTransactionRequest_rawMessage_containsCheckingAccountCode() {
        String raw = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(100), AccountType.CHECKING).getRawMessage();
        assertTrue(raw.contains("10"));  // CHECKING ndcCode
    }

    @Test
    void buildTransactionRequest_rawMessage_containsPan() {
        String raw = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(100), AccountType.CHECKING).getRawMessage();
        assertTrue(raw.contains("4111111111111111"));
    }

    @Test
    void buildTransactionRequest_savingsAccountCode() {
        String raw = builder.buildTransactionRequest("4111111111111111", "1234",
                BigDecimal.valueOf(50), AccountType.SAVINGS).getRawMessage();
        assertTrue(raw.contains("20"));  // SAVINGS ndcCode
    }

    // ── buildBalanceInquiry ──────────────────────────────────────────────────

    @Test
    void buildBalanceInquiry_subClass_isT() {
        NdcMessage msg = builder.buildBalanceInquiry("4111111111111111", "1234", AccountType.CHECKING);
        assertEquals("T", msg.getMessageSubClass());
    }

    @Test
    void buildBalanceInquiry_rawMessage_containsBalanceTxnCode() {
        String raw = builder.buildBalanceInquiry("4111111111111111", "1234", AccountType.CHECKING)
                .getRawMessage();
        assertTrue(raw.contains("010000"));
    }

    @Test
    void buildBalanceInquiry_rawMessage_containsZeroAmount() {
        String raw = builder.buildBalanceInquiry("4111111111111111", "1234", AccountType.CHECKING)
                .getRawMessage();
        assertTrue(raw.contains("000000000000"));
    }

    @Test
    void buildBalanceInquiry_messageClass_isUnsolicited() {
        assertEquals(NdcMessageClass.UNSOLICITED,
                builder.buildBalanceInquiry("4111111111111111", "1234", AccountType.CHECKING).getMessageClass());
    }

    // ── buildTransferRequest ─────────────────────────────────────────────────

    @Test
    void buildTransferRequest_subClass_isT() {
        NdcMessage msg = builder.buildTransferRequest("4111111111111111", "1234",
                BigDecimal.valueOf(50), AccountType.CHECKING, "9876543210");
        assertEquals("T", msg.getMessageSubClass());
    }

    @Test
    void buildTransferRequest_rawMessage_containsTransferTxnCode() {
        String raw = builder.buildTransferRequest("4111111111111111", "1234",
                BigDecimal.valueOf(50), AccountType.CHECKING, "9876543210").getRawMessage();
        assertTrue(raw.contains("030000"));
    }

    @Test
    void buildTransferRequest_rawMessage_containsToAccountNumber() {
        String raw = builder.buildTransferRequest("4111111111111111", "1234",
                BigDecimal.valueOf(50), AccountType.CHECKING, "9876543210").getRawMessage();
        assertTrue(raw.contains("9876543210"));
    }

    @Test
    void buildTransferRequest_rawMessage_containsAmountIn12DigitCents() {
        // $50 → 000000005000
        String raw = builder.buildTransferRequest("4111111111111111", "1234",
                BigDecimal.valueOf(50), AccountType.CHECKING, "9876543210").getRawMessage();
        assertTrue(raw.contains("000000005000"));
    }

    // ── buildLogout ──────────────────────────────────────────────────────────

    @Test
    void buildLogout_messageClass_isUnsolicited() {
        assertEquals(NdcMessageClass.UNSOLICITED, builder.buildLogout().getMessageClass());
    }

    @Test
    void buildLogout_subClass_isB() {
        assertEquals("B", builder.buildLogout().getMessageSubClass());
    }

    @Test
    void buildLogout_direction_isTerminalToHost() {
        assertEquals("TERMINAL->HOST", builder.buildLogout().getDirection());
    }

    @Test
    void buildLogout_rawMessage_containsCardEjected() {
        assertTrue(builder.buildLogout().getRawMessage().contains("CARD_EJECTED"));
    }

    @Test
    void buildLogout_rawMessage_containsTerminalId() {
        assertTrue(builder.buildLogout().getRawMessage().contains("00000001"));
    }

    // ── readable ────────────────────────────────────────────────────────────

    @Test
    void readable_replacesFsWithToken() {
        String readable = builder.buildSolicitedReady().readable();
        assertTrue(readable.contains("<FS>"));
        assertFalse(readable.contains(String.valueOf(NdcDelimiter.FS)));
    }
}
