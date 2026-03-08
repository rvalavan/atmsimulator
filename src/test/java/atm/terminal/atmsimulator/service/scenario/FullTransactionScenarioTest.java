package atm.terminal.atmsimulator.service.scenario;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.model.request.ScenarioRequest;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import atm.terminal.atmsimulator.service.operation.BalanceInquiryOperation;
import atm.terminal.atmsimulator.service.operation.LoginOperation;
import atm.terminal.atmsimulator.service.operation.LogoutOperation;
import atm.terminal.atmsimulator.service.operation.TransferOperation;
import atm.terminal.atmsimulator.service.operation.WithdrawOperation;
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
class FullTransactionScenarioTest {

    @Mock private LoginOperation loginOp;
    @Mock private BalanceInquiryOperation balanceOp;
    @Mock private TransferOperation transferOp;
    @Mock private WithdrawOperation withdrawOp;
    @Mock private LogoutOperation logoutOp;

    @InjectMocks
    private FullTransactionScenario scenario;

    private NdcMessage ndcMsg() {
        return NdcMessage.builder().messageClass(NdcMessageClass.SOLICITED)
                .messageSubClass("F").direction("HOST->TERMINAL")
                .rawMessage("test").timestamp(Instant.now()).build();
    }

    private OperationResult successOp(String name) {
        return OperationResult.builder().operation(name).success(true)
                .status("APPROVED").ndcMessages(List.of(ndcMsg(), ndcMsg())).build();
    }

    private OperationResult balanceResult() {
        return OperationResult.builder().operation("BALANCE").success(true)
                .status("APPROVED").balance(BigDecimal.valueOf(1000))
                .ndcMessages(List.of(ndcMsg(), ndcMsg())).build();
    }

    private ScenarioRequest request() {
        ScenarioRequest req = new ScenarioRequest();
        req.setCardNumber("4111111111111111");
        req.setExpiryDate("2812");
        req.setPin("1234");
        req.setAccountType(AccountType.CHECKING);
        req.setToAccountNumber("9876543210");
        req.setTransferAmount(BigDecimal.valueOf(50));
        req.setWithdrawAmount(BigDecimal.valueOf(100));
        return req;
    }

    private void setupAllSuccessful() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceResult());
        when(transferOp.execute(any(), any(), any())).thenReturn(successOp("TRANSFER"));
        when(withdrawOp.execute(any(), any())).thenReturn(successOp("WITHDRAW"));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));
    }

    @Test
    void execute_scenarioNameIsFullTransaction() {
        setupAllSuccessful();
        assertEquals("FULL_TRANSACTION", scenario.execute(request()).getScenario());
    }

    @Test
    void execute_allSuccess_scenarioIsSuccess() {
        setupAllSuccessful();
        assertTrue(scenario.execute(request()).isSuccess());
    }

    @Test
    void execute_returns6Operations() {
        setupAllSuccessful();
        assertEquals(6, scenario.execute(request()).getOperations().size());
    }

    @Test
    void execute_operationOrder_isLoginBalanceTransferWithdrawBalanceLogout() {
        setupAllSuccessful();
        List<OperationResult> ops = scenario.execute(request()).getOperations();
        assertEquals("LOGIN",    ops.get(0).getOperation());
        assertEquals("BALANCE",  ops.get(1).getOperation());
        assertEquals("TRANSFER", ops.get(2).getOperation());
        assertEquals("WITHDRAW", ops.get(3).getOperation());
        assertEquals("BALANCE",  ops.get(4).getOperation());
        assertEquals("LOGOUT",   ops.get(5).getOperation());
    }

    @Test
    void execute_balanceInquiryCalledTwice() {
        setupAllSuccessful();
        scenario.execute(request());
        verify(balanceOp, times(2)).execute(any());
    }

    @Test
    void execute_withdrawCalledWithCorrectAmount() {
        setupAllSuccessful();
        scenario.execute(request());
        verify(withdrawOp).execute(any(), eq(BigDecimal.valueOf(100)));
    }

    @Test
    void execute_transferCalledWithCorrectParams() {
        setupAllSuccessful();
        scenario.execute(request());
        verify(transferOp).execute(any(), eq("9876543210"), eq(BigDecimal.valueOf(50)));
    }

    @Test
    void execute_withdrawFails_scenarioNotSuccess() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceResult());
        when(transferOp.execute(any(), any(), any())).thenReturn(successOp("TRANSFER"));
        when(withdrawOp.execute(any(), any())).thenReturn(
                OperationResult.builder().operation("WITHDRAW").success(false)
                        .status("DECLINED").ndcMessages(List.of()).build());
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        assertFalse(scenario.execute(request()).isSuccess());
    }

    @Test
    void execute_fullNdcTraceIsNotNull() {
        setupAllSuccessful();
        // Mocked operations don't add to session trace; verify it is at least non-null
        assertNotNull(scenario.execute(request()).getFullNdcTrace());
    }
}
