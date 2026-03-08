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
class TransferAndBalanceScenarioTest {

    @Mock private LoginOperation loginOp;
    @Mock private BalanceInquiryOperation balanceOp;
    @Mock private TransferOperation transferOp;
    @Mock private LogoutOperation logoutOp;

    @InjectMocks
    private TransferAndBalanceScenario scenario;

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
        return req;
    }

    private void setupAllSuccessful() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceResult());
        when(transferOp.execute(any(), any(), any())).thenReturn(successOp("TRANSFER"));
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));
    }

    @Test
    void execute_scenarioNameIsTransferAndBalance() {
        setupAllSuccessful();
        assertEquals("TRANSFER_AND_BALANCE", scenario.execute(request()).getScenario());
    }

    @Test
    void execute_allSuccess_scenarioIsSuccess() {
        setupAllSuccessful();
        assertTrue(scenario.execute(request()).isSuccess());
    }

    @Test
    void execute_returns5Operations() {
        setupAllSuccessful();
        assertEquals(5, scenario.execute(request()).getOperations().size());
    }

    @Test
    void execute_operationOrder_isLoginBalanceTransferBalanceLogout() {
        setupAllSuccessful();
        List<OperationResult> ops = scenario.execute(request()).getOperations();
        assertEquals("LOGIN",    ops.get(0).getOperation());
        assertEquals("BALANCE",  ops.get(1).getOperation());
        assertEquals("TRANSFER", ops.get(2).getOperation());
        assertEquals("BALANCE",  ops.get(3).getOperation());
        assertEquals("LOGOUT",   ops.get(4).getOperation());
    }

    @Test
    void execute_balanceInquiryCalledTwice() {
        setupAllSuccessful();
        scenario.execute(request());
        verify(balanceOp, times(2)).execute(any());
    }

    @Test
    void execute_transferOperationCalledWithCorrectParams() {
        setupAllSuccessful();
        scenario.execute(request());
        verify(transferOp).execute(any(), eq("9876543210"), eq(BigDecimal.valueOf(50)));
    }

    @Test
    void execute_transferFails_scenarioNotSuccess() {
        when(loginOp.execute(any())).thenReturn(successOp("LOGIN"));
        when(balanceOp.execute(any())).thenReturn(balanceResult());
        when(transferOp.execute(any(), any(), any())).thenReturn(
                OperationResult.builder().operation("TRANSFER").success(false)
                        .status("DECLINED").ndcMessages(List.of()).build());
        when(logoutOp.execute(any())).thenReturn(successOp("LOGOUT"));

        assertFalse(scenario.execute(request()).isSuccess());
    }
}
