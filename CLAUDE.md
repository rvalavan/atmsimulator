# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> For full project context, decisions, and NDC protocol detail see:
> - [docs/MEMORY.md](docs/MEMORY.md) — status, key decisions, next TODOs
> - [docs/architecture.md](docs/architecture.md) — package map, NDC message flows, PIN block, Postman samples

## Project Overview

NCR ATM Simulator — a Spring Boot 4.1.0-SNAPSHOT application (Java 21, Maven, Lombok) that simulates an NCR ATM terminal communicating with an NCR ATM host over TCP/IP using the **NDC (NCR Direct Connect)** protocol. Used as a **testing tool** to drive real card authentication, PIN entry, cash withdrawal, and transfer flows against a live or simulated host.

## Commands

```bash
# Build
./mvnw clean package

# Run (default port 8080, simulated host)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Single test class / method
./mvnw test -Dtest=SomeTestClass
./mvnw test -Dtest=SomeTestClass#methodName

# Kill port 8080 if already in use (Windows)
netstat -ano | grep ':8080' | awk '{print $5}' | xargs -I{} taskkill //PID {} //F
```

## Architecture

### Implemented Package Structure

```
atm.terminal.atmsimulator
├── controller/       AtmController           POST /api/atm/withdraw
├── model/
│   ├── request/      AtmRequest              JSON input
│   └── response/     AtmResponse             JSON output + ndcTrace
├── domain/           OperationType, AccountType, TerminalState, TransactionResult
├── protocol/         NdcMessage, NdcMessageClass, NdcDelimiter, PinBlockUtil
└── service/
    ├── NdcMessageBuilder                     Builds TERMINAL→HOST NDC messages
    ├── AtmSimulationService                  Orchestrates the 4-step withdrawal flow
    └── gateway/
        ├── AtmHostGateway                    Interface
        ├── SimulatedHostGateway              In-memory host (atm.host.simulated=true, DEFAULT)
        └── RealNdcHostGateway                TCP to real NCR host (atm.host.simulated=false)
```

### NDC Protocol Key Facts

- Delimiters: `FS=\u001C  GS=\u001D  RS=\u001E  US=\u001F`
- Always log/display via `NdcMessage.readable()` or `NdcDelimiter.toReadable()` — never raw
- Transaction request sub-class is **`T`** (not `E`)
- Account type NDC codes are **2-digit**: CHECKING=`10`, SAVINGS=`20`, CREDIT=`30`
- Transaction request field order: `txnCode(020000) → amount(12-digit cents) → accountType → PAN → pinBlock`
- PIN block: ISO 9564 Format 0 (clear-text for simulation; 3DES-encrypted under TWK for production)

### Host Gateway Switch

| `atm.host.simulated` | Bean loaded | Behaviour |
|---|---|---|
| `true` (default) | `SimulatedHostGateway` | In-memory; ≤$500 approved, >$500 declined |
| `false` | `RealNdcHostGateway` | TCP socket to `atm.host.address:atm.host.port` |

### Configuration (`application.properties`)

```properties
atm.host.simulated=true       # toggle host mode
atm.host.address=localhost
atm.host.port=4000
atm.terminal.id=00000001      # 8-digit, must be registered with acquirer for real host
atm.institution.id=0001       # 4-digit
```

## Framework & Stack

- **Framework**: Spring Boot WebMVC (servlet stack), not reactive
- **Base package**: `atm.terminal.atmsimulator`
- **Lombok**: annotation processing configured in `pom.xml`
- **Spring Boot**: 4.1.0-SNAPSHOT — uses `https://repo.spring.io/snapshot`
