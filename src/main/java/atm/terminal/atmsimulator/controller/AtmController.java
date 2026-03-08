package atm.terminal.atmsimulator.controller;

import atm.terminal.atmsimulator.model.request.AtmRequest;
import atm.terminal.atmsimulator.model.response.AtmResponse;
import atm.terminal.atmsimulator.service.AtmSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for ATM simulator operations.
 *
 * <p>Accepts a JSON instruction set describing the user's card, PIN, and
 * intended transaction, then drives the full NDC protocol exchange with the
 * host (real or simulated) and returns the result plus the NDC message trace.
 *
 * <h3>Withdraw $100 from checking — example request</h3>
 * <pre>
 * POST /api/atm/withdraw
 * Content-Type: application/json
 *
 * {
 *   "cardNumber":  "4111111111111111",
 *   "expiryDate":  "2812",
 *   "pin":         "1234",
 *   "operation":   "WITHDRAW",
 *   "amount":      100.00,
 *   "accountType": "CHECKING"
 * }
 * </pre>
 *
 * <h3>Example response</h3>
 * <pre>
 * {
 *   "success":           true,
 *   "status":            "APPROVED",
 *   "authorizationCode": "A3F9C1",
 *   "dispensedAmount":   100.00,
 *   "message":           "APPROVED",
 *   "ndcTrace": [
 *     { "direction": "TERMINAL->HOST", "messageClass": "SOLICITED",    ... },
 *     { "direction": "HOST->TERMINAL", "messageClass": "SOLICITED",    ... },
 *     { "direction": "TERMINAL->HOST", "messageClass": "UNSOLICITED",  ... },
 *     { "direction": "HOST->TERMINAL", "messageClass": "HOST_COMMAND", ... },
 *     { "direction": "TERMINAL->HOST", "messageClass": "UNSOLICITED",  ... },
 *     { "direction": "HOST->TERMINAL", "messageClass": "HOST_DATA",    ... }
 *   ]
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/atm")
@RequiredArgsConstructor
public class AtmController {

    private final AtmSimulationService simulationService;

    /**
     * Simulates a complete ATM cash withdrawal.
     * Returns HTTP 200 in all cases (check {@code success} field for outcome).
     */
    @PostMapping("/withdraw")
    public ResponseEntity<AtmResponse> withdraw(@RequestBody AtmRequest request) {
        AtmResponse response = simulationService.processWithdrawal(request);
        return ResponseEntity.ok(response);
    }
}
