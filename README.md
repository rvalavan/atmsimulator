# NCR ATM Simulator

A Spring Boot application that simulates an NCR ATM terminal communicating with an NCR ATM host over the **NDC (NCR Direct Connect)** protocol. Used as a testing tool to simulate real user operations — card authentication, PIN entry, cash withdrawal, and more — against a live or simulated host.

---

## Requirements

- Java 21
- Maven (or use the included `mvnw` wrapper — no installation needed)

---

## Build

```bash
./mvnw clean package
```

---

## Run

```bash
./mvnw spring-boot:run
```

The server starts on **http://localhost:8080**.

By default, `atm.host.simulated=true` in `application.properties`, so no real NCR host is needed. To connect to a real host, set `atm.host.simulated=false` and configure `atm.host.address` and `atm.host.port`.

---

## Test

```bash
./mvnw test
```

Run a single test class:
```bash
./mvnw test -Dtest=AtmSimulatorApplicationTests
```

---

## API

### POST `/api/atm/withdraw` — Cash Withdrawal

Simulates a complete ATM withdrawal: terminal connect → card insert → PIN entry → host authorization → cash dispense.

#### Request

```http
POST http://localhost:8080/api/atm/withdraw
Content-Type: application/json
```

```json
{
  "cardNumber":  "4111111111111111",
  "expiryDate":  "2812",
  "pin":         "1234",
  "operation":   "WITHDRAW",
  "amount":      100.00,
  "accountType": "CHECKING"
}
```

| Field | Type | Description |
|---|---|---|
| `cardNumber` | String | 13–19 digit PAN (Primary Account Number) |
| `expiryDate` | String | Card expiry in `YYMM` format (e.g. `2812` = Dec 2028) |
| `pin` | String | Cardholder PIN (encoded as ISO 9564 Format 0 PIN block internally) |
| `operation` | Enum | `WITHDRAW` \| `BALANCE_INQUIRY` \| `TRANSFER` \| `DEPOSIT` |
| `amount` | Number | Transaction amount in dollars |
| `accountType` | Enum | `CHECKING` \| `SAVINGS` \| `CREDIT` |

#### Response — Approved

```json
{
  "success": true,
  "status": "APPROVED",
  "authorizationCode": "FEB870",
  "dispensedAmount": 100.00,
  "message": "APPROVED",
  "ndcTrace": [
    {
      "messageClass": "SOLICITED",
      "messageSubClass": "F",
      "direction": "TERMINAL->HOST",
      "rawMessage": "2\u001cF\u001c00000001\u001c0\u001c000",
      "readableMessage": "2<FS>F<FS>00000001<FS>0<FS>000",
      "timestamp": "2026-03-08T02:16:54.283Z"
    },
    {
      "messageClass": "SOLICITED",
      "messageSubClass": "F",
      "direction": "HOST->TERMINAL",
      "rawMessage": "2\u001cF\u001c00000000\u001c0",
      "readableMessage": "2<FS>F<FS>00000000<FS>0",
      "timestamp": "2026-03-08T02:16:54.289Z"
    },
    {
      "messageClass": "UNSOLICITED",
      "messageSubClass": "E",
      "direction": "TERMINAL->HOST",
      "rawMessage": "1\u001cE\u001c00000001\u001c0001\u001d;4111111111111111=2812?",
      "readableMessage": "1<FS>E<FS>00000001<FS>0001<GS>;4111111111111111=2812?",
      "timestamp": "2026-03-08T02:16:54.291Z"
    },
    {
      "messageClass": "HOST_COMMAND",
      "messageSubClass": "8",
      "direction": "HOST->TERMINAL",
      "rawMessage": "3\u001c8\u001c024",
      "readableMessage": "3<FS>8<FS>024",
      "timestamp": "2026-03-08T02:16:54.291Z"
    },
    {
      "messageClass": "UNSOLICITED",
      "messageSubClass": "E",
      "direction": "TERMINAL->HOST",
      "rawMessage": "1\u001cE\u001c00000001\u001c0001\u001c001\u001d041225EEEEEEEEEE\u001d1\u001d000000010000",
      "readableMessage": "1<FS>E<FS>00000001<FS>0001<FS>001<GS>041225EEEEEEEEEE<GS>1<GS>000000010000",
      "timestamp": "2026-03-08T02:16:54.291Z"
    },
    {
      "messageClass": "HOST_DATA",
      "messageSubClass": "A",
      "direction": "HOST->TERMINAL",
      "rawMessage": "4\u001cPENDING",
      "readableMessage": "4<FS>PENDING",
      "timestamp": "2026-03-08T02:16:54.303Z"
    }
  ]
}
```

#### Response — Declined (amount > $500 in simulation mode)

```json
{
  "success": false,
  "status": "DECLINED",
  "authorizationCode": null,
  "dispensedAmount": 0,
  "message": "DECLINED - EXCEEDS LIMIT",
  "ndcTrace": [ ... ]
}
```

---

## NDC Message Trace

Every response includes an `ndcTrace` array showing the full NDC protocol exchange in order. Each entry has:

- `direction` — `TERMINAL->HOST` or `HOST->TERMINAL`
- `messageClass` — `SOLICITED`, `UNSOLICITED`, `HOST_COMMAND`, or `HOST_DATA`
- `rawMessage` — exact on-wire bytes (NDC control characters appear as Unicode escapes in JSON)
- `readableMessage` — human-readable form with `<FS>`, `<GS>`, `<RS>`, `<US>` tokens in place of control characters

The 6-message exchange for a withdrawal:

| # | Direction | Class | Description |
|---|---|---|---|
| 1 | TERMINAL→HOST | SOLICITED | Terminal Solicited Ready |
| 2 | HOST→TERMINAL | SOLICITED | Host Ready Acknowledgement |
| 3 | TERMINAL→HOST | UNSOLICITED | Card Data (Track 2) |
| 4 | HOST→TERMINAL | HOST_COMMAND | Enter PIN command (state 024) |
| 5 | TERMINAL→HOST | UNSOLICITED | Transaction Request (PIN block + amount) |
| 6 | HOST→TERMINAL | HOST_DATA | Authorization Response |

---

## Configuration

`src/main/resources/application.properties`:

```properties
# true  = simulated host (default, no network required)
# false = live TCP connection to NCR host
atm.host.simulated=true

atm.host.address=localhost
atm.host.port=4000

atm.terminal.id=00000001
atm.institution.id=0001
```

---

## Simulated Host Rules

When `atm.host.simulated=true`, the built-in `SimulatedHostGateway` is used:

| Amount | Result |
|---|---|
| ≤ $500.00 | APPROVED (random 6-char auth code) |
| > $500.00 | DECLINED — EXCEEDS LIMIT |

---

## Project Structure

```
src/main/java/atm/terminal/atmsimulator/
├── controller/          AtmController              REST entry point
├── model/
│   ├── request/         AtmRequest                 JSON input model
│   └── response/        AtmResponse                JSON output + NDC trace
├── domain/              OperationType, AccountType, TerminalState, TransactionResult
├── protocol/            NdcMessage, NdcMessageClass, NdcDelimiter, PinBlockUtil
└── service/
    ├── NdcMessageBuilder                            Builds outbound NDC messages
    ├── AtmSimulationService                         Orchestrates the transaction flow
    └── gateway/
        ├── AtmHostGateway                           Interface
        ├── SimulatedHostGateway                     In-memory host (default)
        └── RealNdcHostGateway                       Live TCP to NCR host
```
