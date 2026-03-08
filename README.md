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

By default, `atm.host.simulated=true` in `application.properties`, so no real NCR host is needed. To connect to a real host, see [Connecting to a Real NCR ATM Host](#connecting-to-a-real-ncr-atm-host) below.

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
      "messageSubClass": "T",
      "direction": "TERMINAL->HOST",
      "rawMessage": "1\u001cT\u001c00000001\u001c0001\u001d020000\u001d000000010000\u001d10\u001d4111111111111111\u001d041225EEEEEEEEEE",
      "readableMessage": "1<FS>T<FS>00000001<FS>0001<GS>020000<GS>000000010000<GS>10<GS>4111111111111111<GS>041225EEEEEEEEEE",
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

| # | Direction | Class | Sub-class | Description |
|---|---|---|---|---|
| 1 | TERMINAL→HOST | SOLICITED | F | Terminal Solicited Ready |
| 2 | HOST→TERMINAL | SOLICITED | F | Host Ready Acknowledgement |
| 3 | TERMINAL→HOST | UNSOLICITED | E | Card Data (Track 2) |
| 4 | HOST→TERMINAL | HOST_COMMAND | 8 | Enter PIN command (state 024) |
| 5 | TERMINAL→HOST | UNSOLICITED | **T** | Transaction Request (txnCode + amount + accountType + PAN + PIN block) |
| 6 | HOST→TERMINAL | HOST_DATA | A | Authorization Response |

### Transaction Request (message #5) field breakdown

```
1<FS>T<FS>00000001<FS>0001<GS>020000<GS>000000010000<GS>10<GS>4111111111111111<GS>041225EEEEEEEEEE
│    │   │         │        │         │              │    │                    │
│    │   │         │        │         │              │    │                    └─ PIN block (ISO 9564 Format 0)
│    │   │         │        │         │              │    └─ PAN (card number)
│    │   │         │        │         │              └─ Account type (10 = CHECKING/none)
│    │   │         │        │         └─ Amount in cents, 12 digits ($100.00 = 000000010000)
│    │   │         │        └─ Transaction code (02=withdrawal, 0000=flags)
│    │   │         └─ Institution ID
│    │   └─ Terminal ID
│    └─ Sub-class T = Transaction Request
└─ Class 1 = UNSOLICITED
```

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

## Connecting to a Real NCR ATM Host

Follow these steps to switch the simulator from the built-in simulation to a live NCR ATM host.

### Step 1 — Update `application.properties`

```properties
# Switch to real host
atm.host.simulated=false

# IP address or hostname of the NCR ATM host
atm.host.address=192.168.1.100

# TCP port the host listens on (commonly 4000 or 17000 for NDC)
atm.host.port=4000

# 8-digit terminal ID registered with the host/acquirer
atm.terminal.id=00000001

# 4-digit institution/bank ID assigned by the host
atm.institution.id=0001
```

### Step 2 — Open and close the TCP connection

`RealNdcHostGateway` manages the raw TCP socket. Call `openConnection()` before sending any messages and `closeConnection()` when the session is complete. Currently `AtmSimulationService` delegates all messaging through the `AtmHostGateway` interface — wire `openConnection()` / `closeConnection()` into the service around the transaction steps:

```java
// in AtmSimulationService.processWithdrawal(), before Step 1:
if (hostGateway instanceof RealNdcHostGateway real) {
    real.openConnection();
}

// after Step 5 (card ejected):
if (hostGateway instanceof RealNdcHostGateway real) {
    real.closeConnection();
}
```

### Step 3 — PIN block encryption (mandatory for production)

In simulation mode the PIN block is sent as clear-text ISO 9564 Format 0. A real NCR host **requires the PIN block to be 3DES-encrypted** under the Terminal Working Key (TWK) before transmission.

What needs to change in `PinBlockUtil.java`:

| Step | Current (simulation) | Required (production) |
|---|---|---|
| Build format block | `0 + len + PIN + FFFF…` | same |
| Build PAN block | `0000 + rightmost 12 PAN digits` | same |
| XOR blocks | clear-text result | same |
| Encrypt | **not done** | 3DES-encrypt result under TWK |
| Send | clear-text PIN block | encrypted PIN block |

The TWK is loaded from the HSM (Hardware Security Module) or injected at terminal key-load time. Add the encryption step in `PinBlockUtil.buildPinBlock()` or in `NdcMessageBuilder.buildTransactionRequest()` once you have the TWK available.

### Step 4 — Verify terminal registration

The NCR host maintains a terminal table. Before the simulator can exchange messages, the `atm.terminal.id` and `atm.institution.id` must be pre-registered on the host side. Contact your host/acquirer team for the values assigned to the test terminal.

### Step 5 — Firewall / network access

Ensure the machine running the simulator can reach the host on the configured port:

```bash
# Test connectivity (replace IP and port with your values)
telnet 192.168.1.100 4000
```

### What changes automatically

When `atm.host.simulated=false`, Spring Boot loads `RealNdcHostGateway` instead of `SimulatedHostGateway` — no code changes needed. The same REST endpoint and NDC message format are used; only the transport layer changes from in-memory to TCP.

| | Simulated (`true`) | Real (`false`) |
|---|---|---|
| Transport | In-memory method calls | TCP/IP socket |
| Authorization | Amount ≤ $500 → approve | Real host decision |
| PIN block | Clear-text | Must be 3DES-encrypted under TWK |
| Auth code | Random 6 chars | Issued by host |
| Terminal registration | Not required | Required on host |

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
