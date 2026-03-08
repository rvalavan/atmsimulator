package atm.terminal.atmsimulator.service.operation;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.AtmSession;
import atm.terminal.atmsimulator.domain.TerminalState;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginOperationTest {

    @Mock
    private NdcMessageBuilder messageBuilder;

    @Mock
    private AtmHostGateway hostGateway;

    @InjectMocks
    private LoginOperation loginOperation;

    private NdcMessage msg(NdcMessageClass cls, String sub, String dir) {
        return NdcMessage.builder()
                .messageClass(cls).messageSubClass(sub).direction(dir)
                .rawMessage("test").timestamp(Instant.now()).build();
    }

    private AtmSession session() {
        return AtmSession.builder()
                .cardNumber("4111111111111111").expiryDate("2812")
                .pin("1234").accountType(AccountType.CHECKING).build();
    }

    private void setupMocks() {
        when(messageBuilder.buildSolicitedReady())
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "TERMINAL->HOST"));
        when(messageBuilder.buildCardDataMessage(any(), any()))
                .thenReturn(msg(NdcMessageClass.UNSOLICITED, "E", "TERMINAL->HOST"));
        when(hostGateway.connect(any()))
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "HOST->TERMINAL"));
        when(hostGateway.sendCardData(any()))
                .thenReturn(msg(NdcMessageClass.HOST_COMMAND, "8", "HOST->TERMINAL"));
    }

    @Test
    void execute_operationNameIsLogin() {
        setupMocks();
        OperationResult result = loginOperation.execute(session());
        assertEquals("LOGIN", result.getOperation());
    }

    @Test
    void execute_isSuccess() {
        setupMocks();
        OperationResult result = loginOperation.execute(session());
        assertTrue(result.isSuccess());
    }

    @Test
    void execute_statusIsApproved() {
        setupMocks();
        OperationResult result = loginOperation.execute(session());
        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void execute_ndcMessagesHas4Entries() {
        setupMocks();
        OperationResult result = loginOperation.execute(session());
        assertEquals(4, result.getNdcMessages().size());
    }

    @Test
    void execute_setsSessionStateToPinEntry() {
        setupMocks();
        AtmSession session = session();
        loginOperation.execute(session);
        assertEquals(TerminalState.PIN_ENTRY, session.getState());
    }

    @Test
    void execute_addsMessagesToSessionTrace() {
        setupMocks();
        AtmSession session = session();
        loginOperation.execute(session);
        assertEquals(4, session.getNdcTrace().size());
    }

    @Test
    void execute_callsConnectAndSendCardData() {
        setupMocks();
        loginOperation.execute(session());
        verify(hostGateway).connect(any());
        verify(hostGateway).sendCardData(any());
    }

    @Test
    void execute_shortCardNumber_doesNotThrow() {
        setupMocks();
        AtmSession session = AtmSession.builder()
                .cardNumber("123").expiryDate("2812")
                .pin("1234").accountType(AccountType.CHECKING).build();
        assertDoesNotThrow(() -> loginOperation.execute(session));
    }
}
