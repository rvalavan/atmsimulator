package atm.terminal.atmsimulator.service.scenario;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.model.request.ScenarioRequest;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.model.response.ScenarioResponse;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import atm.terminal.atmsimulator.service.operation.BalanceInquiryOperation;
import atm.terminal.atmsimulator.service.operation.LoginOperation;
import atm.terminal.atmsimulator.service.operation.LogoutOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceCheckScenarioTest {

    @Mock private LoginOperation loginOp;
    @Mock private BalanceInquiryOperation balanceOp;
    @Mock private LogoutOperation logoutOp;

    @InjectMocks
    private BalanceCheckScenario scenario;

    private NdcMessage ndcMsg() {
        return NdcMessage.builder().messageClass(NdcMessageClass.SOLICITED)
                .messageSubClass("F").direction("HOST->TERMINAL")
                .rawMessage("test").timestamp(Instant.now()).build();
    }

    private OperationResult successOp(String name) {
        return OperationResult.builder().operation(name).success(true)
                .status("APPROVED").ndcMessages(List.of(ndcMsg(), ndcMsg())).build();
    }

    private OperationResult balanceOp(BigDecimal balance) {
        return OperationResult.builder().operation("BALANCE").success(true)
                .status("APPROVED").balance(balance)
                .ndcMessages(List.of(ndcMsg(), ndcMsg())).build();
    }

    private ScenarioRequest request() {
        ScenarioRequest req = new ScenarioRequest();
        req.setCardNumber("4111111111111111");
        req.setExpiryDate("2812");
        req.setPin("1234");
        req.setAccountType(AccountType.CHECKING);
        return req;
    }

    @Test
    void execute_scenarioNameIsBalanceCheck() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        ScenarioResponse resp = scenario.execute(request());
        assertEquals("BALANCE_CHECK", resp.getScenario());
    }

    @Test
    void execute_allSuccess_scenarioIsSuccess() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        assertTrue(scenario.execute(request()).isSuccess());
    }

    @Test
    void execute_returns3Operations() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        assertEquals(3, scenario.execute(request()).getOperations().size());
    }

    @Test
    void execute_operationOrderIsLoginBalanceLogout() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        List<OperationResult> ops = scenario.execute(request()).getOperations();
        assertEquals("LOGIN", ops.get(0).getOperation());
        assertEquals("BALANCE", ops.get(1).getOperation());
        assertEquals("LOGOUT", ops.get(2).getOperation());
    }

    @Test
    void execute_fullNdcTraceIsNotNull() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        ScenarioResponse resp = scenario.execute(request());
        // Mocked operations don't add to session trace; verify it is at least non-null
        assertNotNull(resp.getFullNdcTrace());
    }

    @Test
    void execute_oneOperationFails_scenarioNotSuccess() {
        when(loginOp.execute(any())).thenReturn(
                OperationResult.builder().operation("LOGIN").success(false)
                        .status("ERROR").ndcMessages(List.of()).build());
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        assertFalse(scenario.execute(request()).isSuccess());
    }

    @Test
    void execute_allThreeOperationsCalled() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceOp(BigDecimal.valueOf(1000)));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        scenario.execute(request());
        verify(loginOp).execute(any());
        verify(balanceOp).execute(any());
        verify(logoutOp).execute(any());
    }
}
