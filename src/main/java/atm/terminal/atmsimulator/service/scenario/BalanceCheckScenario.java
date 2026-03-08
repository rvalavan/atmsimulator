package atm.terminal.atmsimulator.service.scenario;

import atm.terminal.atmsimulator.domain.AtmSession;
import atm.terminal.atmsimulator.model.request.ScenarioRequest;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.model.response.ScenarioResponse;
import atm.terminal.atmsimulator.service.operation.BalanceInquiryOperation;
import atm.terminal.atmsimulator.service.operation.LoginOperation;
import atm.terminal.atmsimulator.service.operation.LogoutOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario 1 — Login → Balance Inquiry → Logout.
 *
 * <pre>
 * POST /api/atm/scenario/balance-check
 * { "cardNumber": "4111111111111111", "expiryDate": "2812",
 *   "pin": "1234", "accountType": "CHECKING" }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceCheckScenario {

    private final LoginOperation          loginOp;
    private final BalanceInquiryOperation balanceOp;
    private final LogoutOperation         logoutOp;

    public ScenarioResponse execute(ScenarioRequest request) {
        log.info("[SCENARIO-1] Balance Check — card ending {}", last4(request.getCardNumber()));

        AtmSession session = AtmSession.builder()
                .cardNumber(request.getCardNumber())
                .expiryDate(request.getExpiryDate())
                .pin(request.getPin())
                .accountType(request.getAccountType())
                .build();

        List<OperationResult> operations = new ArrayList<>();

        OperationResult login   = loginOp.execute(session);   operations.add(login);
        OperationResult balance = balanceOp.execute(session);  operations.add(balance);
        OperationResult logout  = logoutOp.execute(session);   operations.add(logout);

        boolean allSuccess = operations.stream().allMatch(OperationResult::isSuccess);
        log.info("[SCENARIO-1] Completed — success={}", allSuccess);

        return ScenarioResponse.builder()
                .scenario("BALANCE_CHECK")
                .success(allSuccess)
                .operations(operations)
                .fullNdcTrace(session.getNdcTrace())
                .build();
    }

    private String last4(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4) : "****";
    }
}
