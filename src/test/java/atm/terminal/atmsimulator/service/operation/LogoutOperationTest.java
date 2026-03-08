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
class LogoutOperationTest {

    @Mock
    private NdcMessageBuilder messageBuilder;

    @Mock
    private AtmHostGateway hostGateway;

    @InjectMocks
    private LogoutOperation logoutOperation;

    private NdcMessage msg(NdcMessageClass cls, String sub, String dir) {
        return NdcMessage.builder()
                .messageClass(cls).messageSubClass(sub).direction(dir)
                .rawMessage("test").timestamp(Instant.now()).build();
    }

    private AtmSession session() {
        return AtmSession.builder()
                .cardNumber("4111111111111111").expiryDate("2812")
                .pin("1234").accountType(AccountType.CHECKING)
                .state(TerminalState.PIN_ENTRY)
                .build();
    }

    private void setupMocks() {
        when(messageBuilder.buildLogout())
                .thenReturn(msg(NdcMessageClass.UNSOLICITED, "B", "TERMINAL->HOST"));
        when(hostGateway.sendMessage(any()))
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "HOST->TERMINAL"));
    }

    @Test
    void execute_operationNameIsLogout() {
        setupMocks();
        assertEquals("LOGOUT", logoutOperation.execute(session()).getOperation());
    }

    @Test
    void execute_isSuccess() {
        setupMocks();
        assertTrue(logoutOperation.execute(session()).isSuccess());
    }

    @Test
    void execute_statusIsApproved() {
        setupMocks();
        assertEquals("APPROVED", logoutOperation.execute(session()).getStatus());
    }

    @Test
    void execute_messageContainsCardEjected() {
        setupMocks();
        OperationResult result = logoutOperation.execute(session());
        assertTrue(result.getMessage().contains("Card ejected"));
    }

    @Test
    void execute_setsSessionStateToIdle() {
        setupMocks();
        AtmSession session = session();
        logoutOperation.execute(session);
        assertEquals(TerminalState.IDLE, session.getState());
    }

    @Test
    void execute_ndcMessagesHas2Entries() {
        setupMocks();
        assertEquals(2, logoutOperation.execute(session()).getNdcMessages().size());
    }

    @Test
    void execute_addsMessagesToSessionTrace() {
        setupMocks();
        AtmSession session = session();
        logoutOperation.execute(session);
        assertEquals(2, session.getNdcTrace().size());
    }

    @Test
    void execute_callsBuildLogoutAndSendMessage() {
        setupMocks();
        logoutOperation.execute(session());
        verify(messageBuilder).buildLogout();
        verify(hostGateway).sendMessage(any());
    }
}
