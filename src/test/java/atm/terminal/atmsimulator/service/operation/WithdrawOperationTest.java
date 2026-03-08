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
class WithdrawOperationTest {

    @Mock
    private NdcMessageBuilder messageBuilder;

    @Mock
    private AtmHostGateway hostGateway;

    @InjectMocks
    private WithdrawOperation withdrawOperation;

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
        when(messageBuilder.buildTransactionRequest(any(), any(), any(), any())).thenReturn(msg());
        when(hostGateway.sendTransaction(any())).thenReturn(msg());
        TransactionResult result = approved
                ? TransactionResult.builder().approved(true).authorizationCode("XYZ789")
                        .dispensedAmount(BigDecimal.valueOf(100)).hostMessage("APPROVED").build()
                : TransactionResult.builder().approved(false)
                        .dispensedAmount(BigDecimal.ZERO).hostMessage("DECLINED - EXCEEDS LIMIT").build();
        when(hostGateway.parseAuthorizationResponse(any(), any(), any(), any())).thenReturn(result);
    }

    @Test
    void execute_operationNameIsWithdraw() {
        setupMocks(true);
        assertEquals("WITHDRAW", withdrawOperation.execute(session(), BigDecimal.valueOf(100)).getOperation());
    }

    @Test
    void execute_approved_isSuccess() {
        setupMocks(true);
        assertTrue(withdrawOperation.execute(session(), BigDecimal.valueOf(100)).isSuccess());
    }

    @Test
    void execute_approved_statusIsApproved() {
        setupMocks(true);
        assertEquals("APPROVED", withdrawOperation.execute(session(), BigDecimal.valueOf(100)).getStatus());
    }

    @Test
    void execute_approved_authCodePopulated() {
        setupMocks(true);
        OperationResult result = withdrawOperation.execute(session(), BigDecimal.valueOf(100));
        assertEquals("XYZ789", result.getAuthorizationCode());
    }

    @Test
    void execute_approved_processedAmountPopulated() {
        setupMocks(true);
        OperationResult result = withdrawOperation.execute(session(), BigDecimal.valueOf(100));
        assertEquals(BigDecimal.valueOf(100), result.getProcessedAmount());
    }

    @Test
    void execute_approved_setsStateToDispensing() {
        setupMocks(true);
        AtmSession session = session();
        withdrawOperation.execute(session, BigDecimal.valueOf(100));
        assertEquals(TerminalState.DISPENSING, session.getState());
    }

    @Test
    void execute_declined_isNotSuccess() {
        setupMocks(false);
        assertFalse(withdrawOperation.execute(session(), BigDecimal.valueOf(600)).isSuccess());
    }

    @Test
    void execute_declined_statusIsDeclined() {
        setupMocks(false);
        assertEquals("DECLINED", withdrawOperation.execute(session(), BigDecimal.valueOf(600)).getStatus());
    }

    @Test
    void execute_declined_doesNotSetStateToDispensing() {
        setupMocks(false);
        AtmSession session = session();
        withdrawOperation.execute(session, BigDecimal.valueOf(600));
        assertNotEquals(TerminalState.DISPENSING, session.getState());
    }

    @Test
    void execute_ndcMessagesHas2Entries() {
        setupMocks(true);
        assertEquals(2, withdrawOperation.execute(session(), BigDecimal.valueOf(100)).getNdcMessages().size());
    }

    @Test
    void execute_addsMessagesToSessionTrace() {
        setupMocks(true);
        AtmSession session = session();
        withdrawOperation.execute(session, BigDecimal.valueOf(100));
        assertEquals(2, session.getNdcTrace().size());
    }
}
