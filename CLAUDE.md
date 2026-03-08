# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NCR ATM Simulator — a Spring Boot 4.1.0-SNAPSHOT application (Java 21, Maven, Lombok) that simulates an NCR ATM terminal. It connects to an NCR ATM host over TCP/IP using the **NDC (NCR Direct Connect)** protocol to simulate real user operations: card authentication, PIN entry, balance inquiry, cash withdrawal, and transfers. The primary use case is as a **testing tool** against a live or mock NCR host.

## Commands

```bash
# Build
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SomeTestClass

# Run a single test method
./mvnw test -Dtest=SomeTestClass#methodName
```

## Architecture

### NDC Protocol

NCR ATMs communicate with the host using the **NDC (Network Data Communication)** protocol:
- Messages are ASCII-based, field-separated by specific delimiters (FS = `\x1C`, GS = `\x1D`, RS = `\x1E`, US = `\x1F`)
- Two message directions: **Terminal → Host** (unsolicited/solicited requests) and **Host → Terminal** (command/data responses)
- Key message classes: `Solicited` (terminal status/ready), `Unsolicited` (card data, transaction data), and `Host Command` (screen/state instructions)
- The terminal operates as a **state machine**; the host sends state transitions and screen definitions

### Transaction Flow

```
Terminal connects → sends Terminal State → Host responds with config
→ Card inserted → Terminal sends Card Data (Track 1/2/3)
→ Host sends PIN entry command → Terminal encrypts PIN (EPP/HSM)
→ Terminal sends transaction request (amount, account type)
→ Host responds with Authorized / Declined / Fault
→ Terminal dispenses cash / prints receipt
```

### Key Components to Build

| Package | Responsibility |
|---|---|
| `connection` | TCP socket client that maintains persistent connection to NCR host |
| `protocol.ndc` | NDC message parser/builder (field encoding, delimiters, message classes) |
| `terminal` | State machine representing ATM terminal states |
| `crypto` | PIN block encryption (ISO 9564 Format 0/1/3), HSM simulation |
| `simulation` | Orchestrates user-operation scenarios (withdraw, balance, transfer) |
| `api` | REST endpoints to trigger simulation scenarios (Spring MVC controllers) |

### Protocol Notes

- **Connection**: persistent TCP socket; the terminal sends a `Ready` solicited message on connect and heartbeats periodically
- **PIN Encryption**: PIN blocks must be formatted per ISO 9564 (typically Format 0 XOR'd with PAN) before sending; use a test/dummy key in simulation
- **Card Data**: Track 2 is the minimum required (`; PAN = EXPIRY = SERVICE_CODE ?`)
- **Message Lrc**: NDC messages include an LRC (Longitudinal Redundancy Check) byte at the end
- **Terminal ID / Institution ID**: configured per host environment in `application.properties`

### Configuration (`application.properties`)

```properties
spring.application.name=ATM Simulator
# NCR Host connection
atm.host.address=<host-ip>
atm.host.port=<port>
atm.terminal.id=<terminal-id>
atm.institution.id=<institution-id>
```

## Framework & Stack

- **Framework**: Spring Boot WebMVC (servlet stack), not reactive
- **Base package**: `atm.terminal.atmsimulator`
- **Source root**: `src/main/java/atm/terminal/atmsimulator/`
- **Test root**: `src/test/java/atm/terminal/atmsimulator/`
- **Lombok**: used for boilerplate; annotation processing configured in `pom.xml`
- **Spring Boot version**: 4.1.0-SNAPSHOT — uses Spring snapshot repository (`https://repo.spring.io/snapshot`)
