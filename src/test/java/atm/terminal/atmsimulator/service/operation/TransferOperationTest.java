package atm.terminal.atmsimulator.service.operation;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.AtmSession;
import atm.terminal.atmsimulator.domain.TerminalState;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import atm.terminal.atmsimulator.service.NdcMessageBuilder;
import atm.terminal.atmsimulator.service.gateway.AtmHostGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferOperationTest {

    @Mock
    private NdcMessageBuilder messageBuilder;

    @Mock
    private AtmHostGateway hostGateway;

    @InjectMocks
    private TransferOperation transferOperation;

    private static final String TO_ACCOUNT = "9876543210";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(50);

    private NdcMessage msg() {
        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED).messageSubClass("T")
                .direction("TERMINAL->HOST").rawMessage("test").timestamp(Instant.now()).build();
    }

    private AtmSession session() {
        return AtmSession.builder()
                .cardNumber("4111111111111111").expiryDate("2812")
                .pin("1234").accountType(AccountType.CHECKING).build();
    }

    private void setupMocks(boolean approved) {
        when(messageBuilder.buildTransferRequest(any(), any(), any(), any(), any())).thenReturn(msg());
        when(hostGateway.sendTransaction(any())).thenReturn(msg());
        TransactionResult result = approved
                ? TransactionResult.builder().approved(true).authorizationCode("TRF001")
                        .dispensedAmount(AMOUNT).hostMessage("APPROVED").build()
                : TransactionResult.builder().approved(false)
                        .dispensedAmount(BigDecimal.ZERO).hostMessage("DECLINED").build();
        when(hostGateway.parseAuthorizationResponse(any(), any(), any(), any())).thenReturn(result);
    }

    @Test
    void execute_operationNameIsTransfer() {
        setupMocks(true);
        assertEquals("TRANSFER", transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).getOperation());
    }

    @Test
    void execute_approved_isSuccess() {
        setupMocks(true);
        assertTrue(transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).isSuccess());
    }

    @Test
    void execute_approved_statusIsApproved() {
        setupMocks(true);
        assertEquals("APPROVED", transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).getStatus());
    }

    @Test
    void execute_approved_authCodePopulated() {
        setupMocks(true);
        assertEquals("TRF001", transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).getAuthorizationCode());
    }

    @Test
    void execute_approved_processedAmountEqualsTransferAmount() {
        setupMocks(true);
        assertEquals(AMOUNT, transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).getProcessedAmount());
    }

    @Test
    void execute_declined_isNotSuccess() {
        setupMocks(false);
        assertFalse(transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).isSuccess());
    }

    @Test
    void execute_declined_statusIsDeclined() {
        setupMocks(false);
        assertEquals("DECLINED", transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).getStatus());
    }

    @Test
    void execute_declined_processedAmountIsZero() {
        setupMocks(false);
        OperationResult result = transferOperation.execute(session(), TO_ACCOUNT, AMOUNT);
        assertEquals(BigDecimal.ZERO, result.getProcessedAmount());
    }

    @Test
    void execute_setsStateToTransactionProcessing() {
        setupMocks(true);
        AtmSession session = session();
        transferOperation.execute(session, TO_ACCOUNT, AMOUNT);
        assertEquals(TerminalState.TRANSACTION_PROCESSING, session.getState());
    }

    @Test
    void execute_ndcMessagesHas2Entries() {
        setupMocks(true);
        assertEquals(2, transferOperation.execute(session(), TO_ACCOUNT, AMOUNT).getNdcMessages().size());
    }

    @Test
    void execute_addsMessagesToSessionTrace() {
        setupMocks(true);
        AtmSession session = session();
        transferOperation.execute(session, TO_ACCOUNT, AMOUNT);
        assertEquals(2, session.getNdcTrace().size());
    }
}
