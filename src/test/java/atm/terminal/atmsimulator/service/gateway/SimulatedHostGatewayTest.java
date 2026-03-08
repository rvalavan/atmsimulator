package atm.terminal.atmsimulator.service.gateway;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedHostGatewayTest {

    private SimulatedHostGateway gateway;

    private static final String CARD = "4111111111111111";

    @BeforeEach
    void setUp() {
        gateway = new SimulatedHostGateway();
    }

    private NdcMessage dummyMsg() {
        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass("T")
                .direction("TERMINAL->HOST")
                .rawMessage("test")
                .timestamp(Instant.now())
                .build();
    }

    // ── connect ──────────────────────────────────────────────────────────────

    @Test
    void connect_returnsSolicitedAck() {
        NdcMessage ack = gateway.connect(dummyMsg());
        assertEquals(NdcMessageClass.SOLICITED, ack.getMessageClass());
        assertEquals("F", ack.getMessageSubClass());
        assertEquals("HOST->TERMINAL", ack.getDirection());
    }

    @Test
    void connect_rawMessageStartsWith2() {
        NdcMessage ack = gateway.connect(dummyMsg());
        assertEquals("2", ack.getRawMessage().substring(0, 1));
    }

    // ── sendCardData ─────────────────────────────────────────────────────────

    @Test
    void sendCardData_returnsHostCommand() {
        NdcMessage resp = gateway.sendCardData(dummyMsg());
        assertEquals(NdcMessageClass.HOST_COMMAND, resp.getMessageClass());
        assertEquals("8", resp.getMessageSubClass());
    }

    @Test
    void sendCardData_rawMessageContains024() {
        NdcMessage resp = gateway.sendCardData(dummyMsg());
        assertTrue(resp.getRawMessage().contains("024"));
    }

    // ── sendTransaction ──────────────────────────────────────────────────────

    @Test
    void sendTransaction_returnsHostData() {
        NdcMessage resp = gateway.sendTransaction(dummyMsg());
        assertEquals(NdcMessageClass.HOST_DATA, resp.getMessageClass());
        assertEquals("A", resp.getMessageSubClass());
    }

    @Test
    void sendTransaction_rawMessageContainsPending() {
        NdcMessage resp = gateway.sendTransaction(dummyMsg());
        assertTrue(resp.getRawMessage().contains("PENDING"));
    }

    // ── sendMessage ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_returnsSolicitedAck() {
        NdcMessage ack = gateway.sendMessage(dummyMsg());
        assertEquals(NdcMessageClass.SOLICITED, ack.getMessageClass());
        assertEquals("F", ack.getMessageSubClass());
        assertEquals("HOST->TERMINAL", ack.getDirection());
    }

    // ── parseAuthorizationResponse — approval logic ──────────────────────────

    @Test
    void parseAuthorizationResponse_amount100_isApproved() {
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), BigDecimal.valueOf(100), AccountType.CHECKING, CARD);
        assertTrue(result.isApproved());
        assertEquals("APPROVED", result.getHostMessage());
    }

    @Test
    void parseAuthorizationResponse_exactly500_isApproved() {
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), BigDecimal.valueOf(500), AccountType.CHECKING, CARD);
        assertTrue(result.isApproved());
    }

    @Test
    void parseAuthorizationResponse_501_isDeclined() {
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), BigDecimal.valueOf(501), AccountType.CHECKING, CARD);
        assertFalse(result.isApproved());
        assertTrue(result.getHostMessage().contains("DECLINED"));
    }

    @Test
    void parseAuthorizationResponse_1000_isDeclined() {
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), BigDecimal.valueOf(1000), AccountType.CHECKING, CARD);
        assertFalse(result.isApproved());
    }

    @Test
    void parseAuthorizationResponse_approved_authCodeIsNotNull() {
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), BigDecimal.valueOf(100), AccountType.CHECKING, CARD);
        assertNotNull(result.getAuthorizationCode());
        assertEquals(6, result.getAuthorizationCode().length());
    }

    @Test
    void parseAuthorizationResponse_declined_dispensedAmountIsZero() {
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), BigDecimal.valueOf(600), AccountType.CHECKING, CARD);
        assertEquals(BigDecimal.ZERO, result.getDispensedAmount());
    }

    @Test
    void parseAuthorizationResponse_approved_dispensedAmountEqualsRequested() {
        BigDecimal requested = BigDecimal.valueOf(200);
        TransactionResult result = gateway.parseAuthorizationResponse(
                dummyMsg(), requested, AccountType.CHECKING, CARD);
        assertEquals(requested, result.getDispensedAmount());
    }

    // ── balance tracking ─────────────────────────────────────────────────────

    @Test
    void parseBalanceResponse_newCard_returnsInitial1000() {
        TransactionResult result = gateway.parseBalanceResponse(dummyMsg(), CARD);
        assertTrue(result.isApproved());
        assertEquals(BigDecimal.valueOf(1000), result.getCurrentBalance());
    }

    @Test
    void parseBalanceResponse_afterApprovedWithdrawal_balanceDeducted() {
        // Initialize balance first (mirrors real scenario: balance check → withdrawal)
        gateway.parseBalanceResponse(dummyMsg(), CARD);  // initialises to 1000
        gateway.parseAuthorizationResponse(dummyMsg(), BigDecimal.valueOf(100), AccountType.CHECKING, CARD);
        TransactionResult balance = gateway.parseBalanceResponse(dummyMsg(), CARD);
        assertEquals(BigDecimal.valueOf(900), balance.getCurrentBalance());
    }

    @Test
    void parseBalanceResponse_afterDeclinedWithdrawal_balanceUnchanged() {
        // Declined (>$500) should not deduct balance
        gateway.parseBalanceResponse(dummyMsg(), CARD);  // initialise to 1000
        gateway.parseAuthorizationResponse(dummyMsg(), BigDecimal.valueOf(600), AccountType.CHECKING, CARD);
        TransactionResult balance = gateway.parseBalanceResponse(dummyMsg(), CARD);
        assertEquals(BigDecimal.valueOf(1000), balance.getCurrentBalance());
    }

    @Test
    void parseBalanceResponse_differentCards_trackedIndependently() {
        String card2 = "5500000000000004";
        // Initialise both cards, then deduct from card 1 only
        gateway.parseBalanceResponse(dummyMsg(), CARD);   // 1000
        gateway.parseBalanceResponse(dummyMsg(), card2);  // 1000
        gateway.parseAuthorizationResponse(dummyMsg(), BigDecimal.valueOf(100), AccountType.CHECKING, CARD);

        TransactionResult b1 = gateway.parseBalanceResponse(dummyMsg(), CARD);
        TransactionResult b2 = gateway.parseBalanceResponse(dummyMsg(), card2);

        assertEquals(BigDecimal.valueOf(900), b1.getCurrentBalance());
        assertEquals(BigDecimal.valueOf(1000), b2.getCurrentBalance());
    }

    @Test
    void parseBalanceResponse_hostMessage_isBalanceRetrieved() {
        TransactionResult result = gateway.parseBalanceResponse(dummyMsg(), CARD);
        assertEquals("BALANCE RETRIEVED", result.getHostMessage());
    }

    @Test
    void parseAuthorizationResponse_multipleApprovals_cumulativeDeduction() {
        // Initialise balance first, then make two withdrawals
        gateway.parseBalanceResponse(dummyMsg(), CARD);  // 1000
        gateway.parseAuthorizationResponse(dummyMsg(), BigDecimal.valueOf(100), AccountType.CHECKING, CARD);
        gateway.parseAuthorizationResponse(dummyMsg(), BigDecimal.valueOf(200), AccountType.CHECKING, CARD);
        TransactionResult balance = gateway.parseBalanceResponse(dummyMsg(), CARD);
        assertEquals(BigDecimal.valueOf(700), balance.getCurrentBalance());
    }
}
