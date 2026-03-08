package atm.terminal.atmsimulator.service.scenario;

import atm.terminal.atmsimulator.domain.AtmSession;
import atm.terminal.atmsimulator.model.request.ScenarioRequest;
import atm.terminal.atmsimulator.model.response.OperationResult;
import atm.terminal.atmsimulator.model.response.ScenarioResponse;
import atm.terminal.atmsimulator.service.operation.BalanceInquiryOperation;
import atm.terminal.atmsimulator.service.operation.LoginOperation;
import atm.terminal.atmsimulator.service.operation.LogoutOperation;
import atm.terminal.atmsimulator.service.operation.TransferOperation;
import atm.terminal.atmsimulator.service.operation.WithdrawOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario 3 — Login → Balance → Transfer → Withdraw → Balance → Logout.
 *
 * <pre>
 * POST /api/atm/scenario/full-transaction
 * { "cardNumber": "4111111111111111", "expiryDate": "2812",
 *   "pin": "1234", "accountType": "CHECKING",
 *   "toAccountNumber": "9876543210", "transferAmount": 50.00,
 *   "withdrawAmount": 100.00 }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FullTransactionScenario {

    private final LoginOperation          loginOp;
    private final BalanceInquiryOperation balanceOp;
    private final TransferOperation       transferOp;
    private final WithdrawOperation       withdrawOp;
    private final LogoutOperation         logoutOp;

    public ScenarioResponse execute(ScenarioRequest request) {
        log.info("[SCENARIO-3] Full Transaction — card ending {} transfer ${} withdraw ${}",
                last4(request.getCardNumber()), request.getTransferAmount(), request.getWithdrawAmount());

        AtmSession session = AtmSession.builder()
                .cardNumber(request.getCardNumber())
                .expiryDate(request.getExpiryDate())
                .pin(request.getPin())
                .accountType(request.getAccountType())
                .build();

        List<OperationResult> operations = new ArrayList<>();

        OperationResult login           = loginOp.execute(session);                                              operations.add(login);
        OperationResult balanceBefore   = balanceOp.execute(session);                                            operations.add(balanceBefore);
        OperationResult transfer        = transferOp.execute(session, request.getToAccountNumber(), request.getTransferAmount());  operations.add(transfer);
        OperationResult withdraw        = withdrawOp.execute(session, request.getWithdrawAmount());              operations.add(withdraw);
        OperationResult balanceAfter    = balanceOp.execute(session);                                            operations.add(balanceAfter);
        OperationResult logout          = logoutOp.execute(session);                                             operations.add(logout);

        boolean allSuccess = operations.stream().allMatch(OperationResult::isSuccess);
        log.info("[SCENARIO-3] Completed — success={}", allSuccess);

        return ScenarioResponse.builder()
                .scenario("FULL_TRANSACTION")
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
