# ATM Simulator — Architecture & Context

## What This Project Is
NCR ATM terminal simulator. Accepts JSON instructions via REST, drives a complete
NDC (NCR Direct Connect) protocol exchange with a host (simulated or real TCP),
and returns the full NDC message trace in the response. Primary use: testing tool
for ATM host teams.

## Package Structure
```
atm.terminal.atmsimulator
├── controller/
│   └── AtmController              POST /api/atm/withdraw
├── model/
│   ├── request/AtmRequest         JSON input (cardNumber, expiryDate, pin, operation, amount, accountType)
│   └── response/AtmResponse       JSON output (success, status, authCode, dispensedAmount, ndcTrace)
├── domain/
│   ├── OperationType              WITHDRAW | BALANCE_INQUIRY | TRANSFER | DEPOSIT
│   ├── AccountType                CHECKING(10) | SAVINGS(20) | CREDIT(30)  ← 2-digit NDC codes
│   ├── TerminalState              IDLE→CARD_READ→PIN_ENTRY→TRANSACTION_PROCESSING→DISPENSING→CARD_EJECTED
│   └── TransactionResult          approved, authorizationCode, dispensedAmount, hostMessage
├── protocol/
│   ├── NdcDelimiter               FS=\u001C  GS=\u001D  RS=\u001E  US=\u001F
│   │                              + toReadable(String) → replaces with <FS> <GS> <RS> <US>
│   ├── NdcMessageClass            UNSOLICITED(1) | SOLICITED(2) | HOST_COMMAND(3) | HOST_DATA(4)
│   ├── NdcMessage                 messageClass, messageSubClass, direction, rawMessage, timestamp
│   │                              + readable() → @JsonProperty("readableMessage") in JSON response
│   └── PinBlockUtil               ISO 9564 Format 0 clear-text PIN block (XOR format+PAN blocks)
└── service/
    ├── NdcMessageBuilder          Builds all outbound TERMINAL→HOST NDC messages
    ├── AtmSimulationService       Orchestrates the 4-step withdrawal flow
    └── gateway/
        ├── AtmHostGateway         Interface: connect(), sendCardData(), sendTransaction(), parseAuthorizationResponse()
        ├── SimulatedHostGateway   @ConditionalOnProperty(atm.host.simulated=true)  ← DEFAULT
        └── RealNdcHostGateway     @ConditionalOnProperty(atm.host.simulated=false) ← TCP to real host
```

## NDC Protocol — Withdrawal Flow (6 messages)

| # | Direction | Class | Sub | Readable form |
|---|---|---|---|---|
| 1 | T→H | SOLICITED | F | `2<FS>F<FS>terminalId<FS>0<FS>000` |
| 2 | H→T | SOLICITED | F | `2<FS>F<FS>00000000<FS>0` |
| 3 | T→H | UNSOLICITED | E | `1<FS>E<FS>terminalId<FS>instId<GS>;PAN=YYMM?` |
| 4 | H→T | HOST_COMMAND | 8 | `3<FS>8<FS>024` |
| 5 | T→H | UNSOLICITED | **T** | `1<FS>T<FS>terminalId<FS>instId<GS>020000<GS>amount12<GS>acctType<GS>PAN<GS>pinBlock` |
| 6 | H→T | HOST_DATA | A | `4<FS>PENDING` (sim) / real auth response |

## Transaction Request (msg #5) Field Detail
```
txnCode    = 020000  (02=withdrawal, 0000=flags)
amount     = 12-digit zero-padded cents  ($100 → 000000010000)
acctType   = 2-digit (from+to)  CHECKING=10, SAVINGS=20, CREDIT=30
PAN        = full card number
pinBlock   = ISO 9564 Fmt 0: formatBlock XOR panBlock (16 hex chars, clear-text in sim)
```

## PIN Block (ISO 9564 Format 0)
```
formatBlock = "0" + hex(len(PIN)) + PIN + "FFFF…" (pad to 16 hex)
panBlock    = "0000" + rightmost 12 digits of PAN excluding check digit
pinBlock    = formatBlock XOR panBlock
Example: PIN=1234, PAN=4111111111111111
  formatBlock = 041234FFFFFFFFFF
  panBlock    = 0000111111111111
  pinBlock    = 041225EEEEEEEEEE
```
⚠️ Clear-text only for simulation. Real host requires 3DES encryption under TWK.

## Configuration (`application.properties`)
```properties
atm.host.simulated=true          # false = TCP to real host
atm.host.address=localhost        # real host IP
atm.host.port=4000                # real host port (commonly 4000 or 17000)
atm.terminal.id=00000001          # 8-digit terminal ID (registered with acquirer)
atm.institution.id=0001           # 4-digit institution ID
```

## Switching to Real Host
1. Set `atm.host.simulated=false`
2. Set `atm.host.address` and `atm.host.port`
3. Confirm `atm.terminal.id` and `atm.institution.id` are registered on the host
4. Wire `openConnection()` / `closeConnection()` from `RealNdcHostGateway` in `AtmSimulationService`
5. Add 3DES PIN block encryption in `PinBlockUtil` using the Terminal Working Key (TWK)

## Running & Testing
```bash
./mvnw spring-boot:run            # start server on :8080
./mvnw test                       # run all tests

# Kill port 8080 if already in use (Windows):
netstat -ano | grep ':8080' | awk '{print $5}' | xargs -I{} taskkill //PID {} //F
```

## Postman — Withdraw $100
```
POST http://localhost:8080/api/atm/withdraw
Content-Type: application/json

{
  "cardNumber":  "4111111111111111",
  "expiryDate":  "2812",
  "pin":         "1234",
  "operation":   "WITHDRAW",
  "amount":      100.00,
  "accountType": "CHECKING"
}
```
Amounts ≤ $500 → APPROVED.  Amounts > $500 → DECLINED (simulated host rule).

## GitHub Commits (latest first)
- `ae159d7` — README: real host connection guide + NDC trace fix
- `edd3c50` — Fix transaction request: sub-class T, field order, 2-digit account type
- `8da8862` — README: build/run/test/API docs
- `6235769` — Initial implementation (withdrawal flow, NDC protocol, simulated host)
- `f3d8a0f` — GitHub initial commit (README stub)
