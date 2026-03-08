package atm.terminal.atmsimulator.controller;

import atm.terminal.atmsimulator.model.response.AtmResponse;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.model.response.ScenarioResponse;
import atm.terminal.atmsimulator.service.AtmSimulationService;
import atm.terminal.atmsimulator.service.scenario.BalanceCheckScenario;
import atm.terminal.atmsimulator.service.scenario.FullTransactionScenario;
import atm.terminal.atmsimulator.service.scenario.TransferAndBalanceScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AtmController.class)
class AtmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AtmSimulationService simulationService;

    @MockitoBean
    private BalanceCheckScenario balanceCheckScenario;

    @MockitoBean
    private TransferAndBalanceScenario transferAndBalanceScenario;

    @MockitoBean
    private FullTransactionScenario fullTransactionScenario;

    private AtmResponse approvedAtmResponse() {
        return AtmResponse.builder()
                .success(true).status("APPROVED")
                .authorizationCode("ABC123")
                .dispensedAmount(BigDecimal.valueOf(100))
                .message("APPROVED").ndcTrace(List.of()).build();
    }

    private ScenarioResponse successScenarioResponse(String name) {
        return ScenarioResponse.builder()
                .scenario(name).success(true)
                .operations(List.of(
                        OperationResult.builder().operation("LOGIN").success(true)
                                .status("APPROVED").ndcMessages(List.of()).build()))
                .fullNdcTrace(List.of()).build();
    }

    // ── POST /api/atm/withdraw ───────────────────────────────────────────────

    @Test
    void withdraw_returnsHttp200() throws Exception {
        when(simulationService.processWithdrawal(any())).thenReturn(approvedAtmResponse());

        mockMvc.perform(post("/api/atm/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "operation": "WITHDRAW",
                                  "amount": 100.00,
                                  "accountType": "CHECKING"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void withdraw_approved_responseBodyContainsSuccess() throws Exception {
        when(simulationService.processWithdrawal(any())).thenReturn(approvedAtmResponse());

        mockMvc.perform(post("/api/atm/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "operation": "WITHDRAW",
                                  "amount": 100.00,
                                  "accountType": "CHECKING"
                                }
                                """))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.authorizationCode").value("ABC123"));
    }

    // ── POST /api/atm/scenario/balance-check ────────────────────────────────

    @Test
    void balanceCheck_returnsHttp200() throws Exception {
        when(balanceCheckScenario.execute(any()))
                .thenReturn(successScenarioResponse("BALANCE_CHECK"));

        mockMvc.perform(post("/api/atm/scenario/balance-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "accountType": "CHECKING"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void balanceCheck_responseBodyContainsScenarioName() throws Exception {
        when(balanceCheckScenario.execute(any()))
                .thenReturn(successScenarioResponse("BALANCE_CHECK"));

        mockMvc.perform(post("/api/atm/scenario/balance-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "accountType": "CHECKING"
                                }
                                """))
                .andExpect(jsonPath("$.scenario").value("BALANCE_CHECK"))
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── POST /api/atm/scenario/transfer-and-balance ──────────────────────────

    @Test
    void transferAndBalance_returnsHttp200() throws Exception {
        when(transferAndBalanceScenario.execute(any()))
                .thenReturn(successScenarioResponse("TRANSFER_AND_BALANCE"));

        mockMvc.perform(post("/api/atm/scenario/transfer-and-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "accountType": "CHECKING",
                                  "toAccountNumber": "9876543210",
                                  "transferAmount": 50.00
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void transferAndBalance_responseBodyContainsScenarioName() throws Exception {
        when(transferAndBalanceScenario.execute(any()))
                .thenReturn(successScenarioResponse("TRANSFER_AND_BALANCE"));

        mockMvc.perform(post("/api/atm/scenario/transfer-and-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "accountType": "CHECKING",
                                  "toAccountNumber": "9876543210",
                                  "transferAmount": 50.00
                                }
                                """))
                .andExpect(jsonPath("$.scenario").value("TRANSFER_AND_BALANCE"))
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── POST /api/atm/scenario/full-transaction ──────────────────────────────

    @Test
    void fullTransaction_returnsHttp200() throws Exception {
        when(fullTransactionScenario.execute(any()))
                .thenReturn(successScenarioResponse("FULL_TRANSACTION"));

        mockMvc.perform(post("/api/atm/scenario/full-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "accountType": "CHECKING",
                                  "toAccountNumber": "9876543210",
                                  "transferAmount": 50.00,
                                  "withdrawAmount": 100.00
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void fullTransaction_responseBodyContainsScenarioName() throws Exception {
        when(fullTransactionScenario.execute(any()))
                .thenReturn(successScenarioResponse("FULL_TRANSACTION"));

        mockMvc.perform(post("/api/atm/scenario/full-transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2812",
                                  "pin": "1234",
                                  "accountType": "CHECKING",
                                  "toAccountNumber": "9876543210",
                                  "transferAmount": 50.00,
                                  "withdrawAmount": 100.00
                                }
                                """))
                .andExpect(jsonPath("$.scenario").value("FULL_TRANSACTION"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.operations").isArray());
    }
}
