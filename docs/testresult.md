# ATM Simulator — Test Results

Last run: 2026-03-08

---

## Summary

| Metric | Result |
|---|---|
| Total tests | **178** |
| Passed | **178** |
| Failed | **0** |
| Skipped | **0** |
| Build status | **SUCCESS** |
| Line coverage | **86.2%** (461 / 535 lines) |

Run command:
```bash
./mvnw test
```

Coverage report: `target/site/jacoco/index.html` (generated automatically after `./mvnw test`)

---

## Coverage by Class

| Class | Lines Covered | Lines Total | Coverage |
|---|---|---|---|
| `NdcMessageBuilder` | 79 | 79 | 100% |
| `SimulatedHostGateway` | 71 | 71 | 100% |
| `PinBlockUtil` | 17 | 17 | 100% |
| `NdcMessageClass` | 16 | 16 | 100% |
| `NdcDelimiter` | 6 | 6 | 100% |
| `NdcMessage` | 1 | 1 | 100% |
| `TransferAndBalanceScenario` | 25 | 25 | 100% |
| `FullTransactionScenario` | 26 | 26 | 100% |
| `BalanceCheckScenario` | 22 | 22 | 100% |
| `TransferOperation` | 25 | 25 | 100% |
| `WithdrawOperation` | 27 | 27 | 100% |
| `BalanceInquiryOperation` | 20 | 20 | 100% |
| `LoginOperation` | 20 | 20 | 100% |
| `LogoutOperation` | 17 | 17 | 100% |
| `AtmController` | 5 | 5 | 100% |
| `AtmSession` | 4 | 4 | 100% |
| `AccountType` | 8 | 8 | 100% |
| `TerminalState` | 9 | 9 | 100% |
| `OperationType` | 5 | 5 | 100% |
| `AtmSimulationService` | 57 | 58 | 98% |
| `AtmSimulatorApplication` | 1 | 3 | 33% |
| `RealNdcHostGateway` | 0 | 71 | 0% ¹ |

> ¹ `RealNdcHostGateway` requires a live TCP socket to an NCR host and is excluded from unit tests by design. It is activated only when `atm.host.simulated=false`.

---

## Test Files

| Test Class | Tests | What It Covers |
|---|---|---|
| `PinBlockUtilTest` | 7 | ISO 9564 Format 0 PIN block — known example, boundary PINs, 16-char result, uppercase output |
| `NdcDelimiterTest` | 9 | FS/GS/RS/US replacement, null input, empty string, plain string passthrough |
| `NdcMessageClassTest` | 11 | `code()` for all 4 classes, `fromCode()` round-trip, unknown code exception |
| `AccountTypeTest` | 5 | NDC codes (`10`/`20`/`30`), 2-digit constraint, 3 enum values |
| `AtmSessionTest` | 8 | Default state IDLE, empty trace, `addMessage`, `addMessages`, state transitions, builder |
| `NdcMessageBuilderTest` | 27 | All 6 builder methods — message class, sub-class, direction, raw content, txnCode, amount encoding |
| `SimulatedHostGatewayTest` | 20 | `connect`, `sendCardData`, `sendTransaction`, `sendMessage`, approval at ≤$500, decline at >$500, auth code, balance init/deduction, multi-card isolation |
| `AtmSimulationServiceTest` | 9 | Approved flow, declined flow, exception handling, 6-message trace, all gateway methods called |
| `LoginOperationTest` | 8 | Operation name, success flag, 4-message trace, state→PIN_ENTRY, gateway calls, short card number |
| `BalanceInquiryOperationTest` | 10 | Operation name, balance field, 2-message trace, state→TRANSACTION_PROCESSING, error path |
| `WithdrawOperationTest` | 11 | Approved/declined paths, auth code, processedAmount, state→DISPENSING on approval, trace |
| `TransferOperationTest` | 12 | Approved/declined paths, processedAmount=0 on decline, auth code, state transition, trace |
| `LogoutOperationTest` | 8 | Operation name, success, state→IDLE, 2-message trace, `buildLogout`+`sendMessage` called |
| `BalanceCheckScenarioTest` | 7 | Scenario name, 3 operations, correct order, success flag, one-failure propagation |
| `TransferAndBalanceScenarioTest` | 7 | Scenario name, 5 operations, correct order, `balanceOp` called twice, transfer params, failure propagation |
| `FullTransactionScenarioTest` | 9 | Scenario name, 6 operations, correct order, `balanceOp` called twice, withdraw/transfer params, failure propagation |
| `AtmControllerTest` | 8 | HTTP 200 for all 4 endpoints, response JSON fields (`success`, `status`, `scenario`, `authorizationCode`) |
| `AtmSimulatorApplicationTests` | 1 | Spring context loads (existing smoke test) |

---

## Key Test Design Decisions

| Decision | Reason |
|---|---|
| Mockito (`@ExtendWith(MockitoExtension.class)`) for service/operation/scenario tests | Fast, isolated unit tests with no Spring context overhead |
| `@WebMvcTest(AtmController.class)` + `@MockitoBean` for controller tests | Loads only the web layer; all service dependencies mocked |
| `ReflectionTestUtils.setField` to inject `@Value` fields in `NdcMessageBuilderTest` | Avoids Spring context while still testing the real builder logic |
| Direct instantiation of `SimulatedHostGateway` in its tests | No constructor injection — class uses a field-initialized `ConcurrentHashMap` |
| Balance tests call `parseBalanceResponse` before `parseAuthorizationResponse` | Mirrors real scenario order: balance is always checked first, which initialises the $1,000 entry via `computeIfAbsent` |
| `RealNdcHostGateway` excluded from unit tests | Requires live TCP connection; excluded by `@ConditionalOnProperty(atm.host.simulated=false)` |
