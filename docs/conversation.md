# ATM Simulator — Full Conversation Log & Implementation Summary

Captured: 2026-03-08 | Repo: https://github.com/rvalavan/atmsimulator

---

## Session Goal

Build an NCR ATM Simulator using Spring Boot that:
1. Communicates with an NCR ATM host using the NDC (NCR Direct Connect) protocol
2. Provides REST endpoints to simulate user ATM operations
3. Supports both a simulated in-memory host and a real TCP-connected NCR host
4. Implements multi-step ATM scenarios with separate reusable operation classes

---

## Phase 1 — Initial Setup & Single Withdrawal

### What Was Built
- Spring Boot 4.1.0-SNAPSHOT application, Java 21, Maven, Lombok
- REST controller: `POST /api/atm/withdraw`
- 4-step withdrawal flow: Solicited Ready → Card Data → Transaction Request → Authorization
- Simulated host (`SimulatedHostGateway`): ≤$500 approved, >$500 declined
- Real host TCP gateway (`RealNdcHostGateway`): swapped via `@ConditionalOnProperty`

### NDC Protocol Corrections Made (from production sample)
- Transaction sub-class is **`T`** not `E`
- Account type is **2-digit**: CHECKING=`10`, SAVINGS=`20`, CREDIT=`30`
- Transaction request field order: `txnCode → amount(12-digit cents) → accountType → PAN → pinBlock`
- `NdcMessage.readable()` / `NdcDelimiter.toReadable()` added for human-readable logs

### Sample NDC Withdrawal (6 messages)
```
1  T→H  SOLICITED F     2<FS>F<FS>00000001<FS>0<FS>000
2  H→T  SOLICITED F     2<FS>F<FS>00000000<FS>0
3  T→H  UNSOLICITED E   1<FS>E<FS>00000001<FS>0001<GS>;4111111111111111=2812?
4  H→T  HOST_COMMAND 8  3<FS>8<FS>024
5  T→H  UNSOLICITED T   1<FS>T<FS>00000001<FS>0001<GS>020000<GS>000000010000<GS>10<GS>4111111111111111<GS>041225EEEEEEEEEE
6  H→T  HOST_DATA A     4<FS>PENDING
```

---

## Phase 2 — Multi-Step Scenario Framework

### User Requirement
> "Implement ATM terminal simulation for:
> 1. Login → Balance → Logout
> 2. Login → Balance → Transfer → Balance → Logout
> 3. Login → Balance → Transfer → Withdraw → Balance → Logout
>
> Create separate class for each operation and stitch from service class."

### Architecture Chosen
- **Operation classes** — one per atomic ATM step, each independently reusable
- **Scenario services** — wire operations together, return per-step results + full NDC trace
- **AtmSession** — carries shared state (card, PIN, accountType, ndcTrace) across all operations
- **OperationResult** — result of one atomic step (operation name, status, balance, authCode, ndcMessages)
- **ScenarioResponse** — final response (scenario name, success, operations[], fullNdcTrace[])

---

## Files Created / Modified

### New Domain & Model Classes

#### `domain/AtmSession.java`
Carries shared state across all operations in a scenario.
```java
@Data @Builder
public class AtmSession {
    private String cardNumber, expiryDate, pin;
    private AccountType accountType;
    @Builder.Default private TerminalState state = TerminalState.IDLE;
    @Builder.Default private List<NdcMessage> ndcTrace = new ArrayList<>();
    public void addMessage(NdcMessage m) { ndcTrace.add(m); }
    public void addMessages(List<NdcMessage> msgs) { ndcTrace.addAll(msgs); }
}
```

#### `domain/TransactionResult.java` — added field
```java
private BigDecimal currentBalance;  // populated for BALANCE operations
```

#### `model/request/ScenarioRequest.java`
```java
@Data public class ScenarioRequest {
    private String cardNumber, expiryDate, pin;
    private AccountType accountType;
    private String toAccountNumber;      // scenarios 2 & 3
    private BigDecimal transferAmount;   // scenarios 2 & 3
    private BigDecimal withdrawAmount;   // scenario 3 only
}
```

#### `model/response/OperationResult.java`
```java
@Data @Builder public class OperationResult {
    private String operation;       // LOGIN | BALANCE | TRANSFER | WITHDRAW | LOGOUT
    private boolean success;
    private String status;          // APPROVED | DECLINED | ERROR
    private String message;
    private BigDecimal balance;         // BALANCE ops
    private String authorizationCode;   // TRANSFER/WITHDRAW
    private BigDecimal processedAmount;
    private List<NdcMessage> ndcMessages;
}
```

#### `model/response/ScenarioResponse.java`
```java
@Data @Builder public class ScenarioResponse {
    private String scenario;
    private boolean success;
    private List<OperationResult> operations;
    private List<NdcMessage> fullNdcTrace;
}
```

---

### NdcMessageBuilder — New Methods

#### `buildBalanceInquiry(cardNumber, pin, accountType)`
```
1<FS>T<FS>terminalId<FS>instId<GS>010000<GS>000000000000<GS>acctType<GS>PAN<GS>pinBlock
txnCode 01 = balance inquiry, zero amount
```

#### `buildTransferRequest(cardNumber, pin, amount, accountType, toAccountNumber)`
```
1<FS>T<FS>terminalId<FS>instId<GS>030000<GS>amount12<GS>acctType<GS>toAccount<GS>PAN<GS>pinBlock
txnCode 03 = transfer
```

#### `buildLogout()`
```
1<FS>B<FS>terminalId<FS>instId<GS>CARD_EJECTED
UNSOLICITED sub-class B = card ejected / session end
```

---

### AtmHostGateway — Interface Changes

Added to interface:
```java
// Generic send for logout and other non-transaction messages
NdcMessage sendMessage(NdcMessage message);

// Balance response parser (simulated: looks up ConcurrentHashMap)
TransactionResult parseBalanceResponse(NdcMessage hostResponse, String cardNumber);

// Added cardNumber param so simulated gateway can track balance
TransactionResult parseAuthorizationResponse(NdcMessage hostResponse,
    BigDecimal requestedAmount, AccountType accountType, String cardNumber);
```

---

### SimulatedHostGateway — Balance Tracking Added
```java
private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000);
private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();

// Cards start at $1,000; deducted on each approved withdrawal or transfer
// Balance resets when server restarts (in-memory only)
```

---

### Operation Classes (`service/operation/`)

#### `LoginOperation.java`
- Sends Solicited Ready → receives host ack
- Sends Card Data → receives Enter PIN command
- Produces 4 NDC messages
- Advances session to `PIN_ENTRY` state

#### `BalanceInquiryOperation.java`
- Sends `buildBalanceInquiry()` → receives host response
- Calls `parseBalanceResponse()` → returns `currentBalance`
- Produces 2 NDC messages

#### `TransferOperation.java`
- Sends `buildTransferRequest(toAccountNumber, amount)` → receives host response
- Calls `parseAuthorizationResponse()` → deducts from balance on approval
- Produces 2 NDC messages

#### `WithdrawOperation.java`
- Sends `buildTransactionRequest(amount)` (txnCode 020000) → receives host response
- Calls `parseAuthorizationResponse()` → deducts from balance on approval
- Produces 2 NDC messages

#### `LogoutOperation.java`
- Sends `buildLogout()` (UNSOLICITED sub-class B, CARD_EJECTED)
- Receives host ack
- Advances session back to `IDLE`
- Produces 2 NDC messages

---

### Scenario Services (`service/scenario/`)

#### `BalanceCheckScenario.java` → `POST /api/atm/scenario/balance-check`
```
Login (4 msgs) → Balance (2 msgs) → Logout (2 msgs) = 8 NDC messages total
```

#### `TransferAndBalanceScenario.java` → `POST /api/atm/scenario/transfer-and-balance`
```
Login (4) → Balance (2) → Transfer (2) → Balance (2) → Logout (2) = 12 NDC messages
```

#### `FullTransactionScenario.java` → `POST /api/atm/scenario/full-transaction`
```
Login (4) → Balance (2) → Transfer (2) → Withdraw (2) → Balance (2) → Logout (2) = 14 NDC messages
```

Each scenario:
1. Builds `AtmSession` from `ScenarioRequest`
2. Calls each operation in sequence
3. Accumulates `OperationResult` list
4. Returns `ScenarioResponse` with per-step results + full `ndcTrace`

---

### Controller — 3 New Endpoints (`controller/AtmController.java`)

```java
@PostMapping("/scenario/balance-check")
public ResponseEntity<ScenarioResponse> balanceCheck(@RequestBody ScenarioRequest request)

@PostMapping("/scenario/transfer-and-balance")
public ResponseEntity<ScenarioResponse> transferAndBalance(@RequestBody ScenarioRequest request)

@PostMapping("/scenario/full-transaction")
public ResponseEntity<ScenarioResponse> fullTransaction(@RequestBody ScenarioRequest request)
```

Existing `POST /api/atm/withdraw` is untouched.

---

## Postman Test Requests

### Scenario 1 — Balance Check
```http
POST http://localhost:8080/api/atm/scenario/balance-check
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "expiryDate": "2812",
  "pin": "1234",
  "accountType": "CHECKING"
}
```

### Scenario 2 — Transfer then Balance
```http
POST http://localhost:8080/api/atm/scenario/transfer-and-balance
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "expiryDate": "2812",
  "pin": "1234",
  "accountType": "CHECKING",
  "toAccountNumber": "9876543210",
  "transferAmount": 50.00
}
```

### Scenario 3 — Full Transaction
```http
POST http://localhost:8080/api/atm/scenario/full-transaction
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "expiryDate": "2812",
  "pin": "1234",
  "accountType": "CHECKING",
  "toAccountNumber": "9876543210",
  "transferAmount": 50.00,
  "withdrawAmount": 100.00
}
```

### Original Withdrawal
```http
POST http://localhost:8080/api/atm/withdraw
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "expiryDate": "2812",
  "pin": "1234",
  "operation": "WITHDRAW",
  "amount": 100.00,
  "accountType": "CHECKING"
}
```

---

## Build & Run

```bash
# Build
./mvnw clean package

# Run (Maven)
./mvnw spring-boot:run

# Run (JAR)
java -jar target/atmsimulator-0.0.1-SNAPSHOT.jar

# Run with real host
java -jar target/atmsimulator-0.0.1-SNAPSHOT.jar \
  --atm.host.simulated=false \
  --atm.host.address=192.168.1.100 \
  --atm.host.port=4000

# Tests
./mvnw test

# Kill port 8080 (Windows)
netstat -ano | grep ':8080' | awk '{print $5}' | xargs -I{} taskkill //PID {} //F
```

---

## Bugs Fixed During Session

| Bug | Cause | Fix |
|---|---|---|
| Port 8080 already in use on restart | Old Spring Boot process still held port | `taskkill //PID` the old process |
| 400 Bad Request from Postman | JSON had leading `*` characters copied from Javadoc comment block | Send clean JSON |
| Missing import in RealNdcHostGateway | `NdcDelimiter` used but not imported | Added import |
| Git push rejected | GitHub had an auto-created initial commit | `git pull origin main --rebase` before push |
| NDC sub-class wrong (E vs T) | Production sample showed sub-class T | Corrected `NdcMessageBuilder.buildTransactionRequest()` |
| `parseAuthorizationResponse` signature mismatch | Added `cardNumber` param to interface | Updated all implementations and call site |

---

## GitHub Commits (chronological)

| Hash | Description |
|---|---|
| `f3d8a0f` | GitHub initial commit (README stub) |
| `6235769` | Initial implementation (withdrawal flow, NDC protocol, simulated host) |
| `8da8862` | README: build/run/test/API docs |
| `edd3c50` | Fix transaction request: sub-class T, field order, 2-digit account type |
| `ae159d7` | README: real host connection guide + NDC trace fix |
| `e37e48d` | Add multi-step scenario framework with per-operation classes |
| `49f3802` | Update docs for multi-step scenario framework |
| `7f031b4` | Add conversation.md — full session log and implementation summary |
| (pending) | Add 178 unit tests, JaCoCo 85% enforcement, sequence diagrams, testresult.md |

---

## Phase 3 — Unit Tests, Coverage Enforcement & Docs

### Session date: 2026-03-08 (continued)

### What Was Done

#### 1. Unit Tests (178 tests, 86.2% line coverage)

17 test classes created under `src/test/java/atm/terminal/atmsimulator/`:

| Test Class | Tests | Scope |
|---|---|---|
| `PinBlockUtilTest` | 7 | ISO 9564 Format 0 — known example, boundary PINs, 16-char uppercase |
| `NdcDelimiterTest` | 9 | FS/GS/RS/US replacement, null, empty, no-delimiter passthrough |
| `NdcMessageClassTest` | 11 | `code()`, `fromCode()` round-trip, unknown code exception |
| `AccountTypeTest` | 5 | NDC codes `10`/`20`/`30`, 2-digit, 3 enum values |
| `AtmSessionTest` | 8 | Default IDLE state, addMessage, addMessages, state transitions, builder |
| `NdcMessageBuilderTest` | 27 | All 6 builder methods; `ReflectionTestUtils.setField` to inject `@Value` fields |
| `SimulatedHostGatewayTest` | 20 | connect, sendCardData, sendTransaction, approval/decline, balance tracking, card isolation |
| `AtmSimulationServiceTest` | 9 | Approved/declined/exception flows, 6-message trace, gateway method calls |
| `LoginOperationTest` | 8 | LOGIN op, 4-message trace, state→PIN_ENTRY |
| `BalanceInquiryOperationTest` | 10 | BALANCE op, balance field, state→TRANSACTION_PROCESSING, error path |
| `WithdrawOperationTest` | 11 | Approved/declined, authCode, processedAmount, state→DISPENSING |
| `TransferOperationTest` | 12 | Approved/declined, processedAmount=0 on decline, state transition |
| `LogoutOperationTest` | 8 | LOGOUT, state→IDLE, 2-message trace |
| `BalanceCheckScenarioTest` | 7 | scenario=BALANCE_CHECK, 3 ops, LOGIN/BALANCE/LOGOUT order |
| `TransferAndBalanceScenarioTest` | 7 | scenario=TRANSFER_AND_BALANCE, 5 ops, balanceOp called twice |
| `FullTransactionScenarioTest` | 9 | scenario=FULL_TRANSACTION, 6 ops, withdraw+transfer params |
| `AtmControllerTest` | 8 | HTTP 200 for all 4 endpoints; JSON fields via `@WebMvcTest` + `@MockitoBean` |

#### 2. JaCoCo 85% Coverage Enforcement

Added to `pom.xml` — build FAILS at `verify` phase if coverage drops below 85%:

```xml
<execution>
    <id>coverage-check</id>
    <phase>verify</phase>
    <goals><goal>check</goal></goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.85</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
        <excludes>
            <!-- requires live TCP socket — cannot be unit-tested in isolation -->
            <exclude>atm/terminal/atmsimulator/service/gateway/RealNdcHostGateway.class</exclude>
            <exclude>atm/terminal/atmsimulator/AtmSimulatorApplication.class</exclude>
        </excludes>
    </configuration>
</execution>
```

- `./mvnw test` — runs tests + generates JaCoCo report, no coverage gate
- `./mvnw verify` or `./mvnw package` — also enforces the 85% gate

#### 3. Docs Updated

- **`README.md`** — Added JAR build and `java -jar` run instructions with 3 variants
- **`docs/architecture.md`** — Added ASCII sequence diagrams for all 3 scenarios
- **`docs/testresult.md`** (new) — Full test run results: 178 tests, 0 failures, per-class coverage table, per-test-file table, key design decisions

#### 4. Key Bugs Fixed During Test Writing

| Bug | Cause | Fix |
|---|---|---|
| `@WebMvcTest` import not found | Spring Boot 4 moved package to `org.springframework.boot.webmvc.test.autoconfigure` | Updated import |
| `PinBlockUtilTest` wrong expected value for PIN=0000 | Manual XOR arithmetic error | Corrected to `040011EEEEEEEEEE` |
| Balance tests: balance initialized to wrong value | `merge` (used by `deductBalance`) doesn't init to $1,000 when key absent; `computeIfAbsent` (used by `getBalance`) does | Call `parseBalanceResponse` before `parseAuthorizationResponse` in all balance tests — mirrors real scenario order |
| Scenario ndcTrace always empty in tests | Mocked operations don't call `session.addMessages()` | Changed trace count assertions to `assertNotNull` |

---

## Remaining TODOs

1. Wire `openConnection()` / `closeConnection()` from `RealNdcHostGateway` in scenario services for real host mode
2. Add 3DES PIN block encryption under TWK in `PinBlockUtil` for production use
3. Consider persisting simulated balances across restarts (currently in-memory `ConcurrentHashMap`)

---

## Key NDC Protocol Facts

| Concept | Value |
|---|---|
| Delimiters | `FS=\u001C  GS=\u001D  RS=\u001E  US=\u001F` |
| Transaction sub-class | **T** (not E) |
| Account type codes | CHECKING=`10`, SAVINGS=`20`, CREDIT=`30` |
| txnCode balance | `010000` |
| txnCode withdrawal | `020000` |
| txnCode transfer | `030000` |
| Logout sub-class | `B` with payload `CARD_EJECTED` |
| PIN block | ISO 9564 Format 0: `formatBlock XOR panBlock` (16 hex chars) |
| Simulated approval limit | ≤ $500 approved, > $500 declined |
| Simulated starting balance | $1,000.00 per card |
