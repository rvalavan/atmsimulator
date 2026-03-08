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
class BalanceInquiryOperationTest {

    @Mock
    private NdcMessageBuilder messageBuilder;

    @Mock
    private AtmHostGateway hostGateway;

    @InjectMocks
    private BalanceInquiryOperation balanceInquiryOperation;

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

    private void setupMocks(BigDecimal balance) {
        when(messageBuilder.buildBalanceInquiry(any(), any(), any())).thenReturn(msg());
        when(hostGateway.sendTransaction(any())).thenReturn(msg());
        when(hostGateway.parseBalanceResponse(any(), any()))
                .thenReturn(TransactionResult.builder()
                        .approved(true).hostMessage("BALANCE RETRIEVED")
                        .currentBalance(balance).build());
    }

    @Test
    void execute_operationNameIsBalance() {
        setupMocks(BigDecimal.valueOf(1000));
        assertEquals("BALANCE", balanceInquiryOperation.execute(session()).getOperation());
    }

    @Test
    void execute_isSuccess() {
        setupMocks(BigDecimal.valueOf(1000));
        assertTrue(balanceInquiryOperation.execute(session()).isSuccess());
    }

    @Test
    void execute_statusIsApproved() {
        setupMocks(BigDecimal.valueOf(1000));
        assertEquals("APPROVED", balanceInquiryOperation.execute(session()).getStatus());
    }

    @Test
    void execute_balanceFieldIsPopulated() {
        setupMocks(BigDecimal.valueOf(750));
        OperationResult result = balanceInquiryOperation.execute(session());
        assertEquals(BigDecimal.valueOf(750), result.getBalance());
    }

    @Test
    void execute_ndcMessagesHas2Entries() {
        setupMocks(BigDecimal.valueOf(1000));
        assertEquals(2, balanceInquiryOperation.execute(session()).getNdcMessages().size());
    }

    @Test
    void execute_setsSessionStateToTransactionProcessing() {
        setupMocks(BigDecimal.valueOf(1000));
        AtmSession session = session();
        balanceInquiryOperation.execute(session);
        assertEquals(TerminalState.TRANSACTION_PROCESSING, session.getState());
    }

    @Test
    void execute_addsMessagesToSessionTrace() {
        setupMocks(BigDecimal.valueOf(1000));
        AtmSession session = session();
        balanceInquiryOperation.execute(session);
        assertEquals(2, session.getNdcTrace().size());
    }

    @Test
    void execute_callsSendTransactionAndParseBalanceResponse() {
        setupMocks(BigDecimal.valueOf(1000));
        balanceInquiryOperation.execute(session());
        verify(hostGateway).sendTransaction(any());
        verify(hostGateway).parseBalanceResponse(any(), any());
    }

    @Test
    void execute_notApproved_returnsErrorStatus() {
        when(messageBuilder.buildBalanceInquiry(any(), any(), any())).thenReturn(msg());
        when(hostGateway.sendTransaction(any())).thenReturn(msg());
        when(hostGateway.parseBalanceResponse(any(), any()))
                .thenReturn(TransactionResult.builder()
                        .approved(false).hostMessage("ERROR").build());

        OperationResult result = balanceInquiryOperation.execute(session());
        assertFalse(result.isSuccess());
        assertEquals("ERROR", result.getStatus());
    }
}
