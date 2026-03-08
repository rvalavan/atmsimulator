# ATM Simulator — Project Memory

## Quick Reference
- **Repo**: https://github.com/rvalavan/atmsimulator
- **Local path**: `c:\smart-brain\app\claude\ATMSimulator`
- **Stack**: Spring Boot 4.1.0-SNAPSHOT, Java 21, Maven, Lombok
- **Default run**: `./mvnw spring-boot:run` → http://localhost:8080
- **Run from JAR**: `java -jar target/atmsimulator-0.0.1-SNAPSHOT.jar`
- **Details**: See [architecture.md](architecture.md)

## Current Status (as of 2026-03-08)
- Single withdrawal flow (`POST /api/atm/withdraw`) working ✓
- 3 multi-step scenario endpoints implemented and tested ✓
- Separate operation class per atomic ATM operation (Login, Balance, Transfer, Withdraw, Logout) ✓
- Separate scenario service per scenario (BalanceCheck, TransferAndBalance, FullTransaction) ✓
- Simulated balance tracking per card ($1,000 initial, deducted on approval) ✓
- NDC message format correct (sub-class T, 2-digit account type, txnCode 01/02/03) ✓
- JAR built: `target/atmsimulator-0.0.1-SNAPSHOT.jar` ✓
- GitHub repo up to date ✓

## Key Decisions Made
- Sub-class `T` (not `E`) for transaction request messages — confirmed from production NDC sample
- Account type is 2-digit NDC code: CHECKING=`10`, SAVINGS=`20`, CREDIT=`30`
- txnCode: `010000`=balance, `020000`=withdrawal, `030000`=transfer
- PIN block: ISO 9564 Format 0, clear-text in simulation (needs 3DES encryption for real host)
- `AtmHostGateway` interface enables zero-code-change swap between simulated and real host via `@ConditionalOnProperty`
- `NdcMessage.readable()` / `NdcDelimiter.toReadable()` used in all logs and JSON — never log `rawMessage` directly
- `AtmSession` carries shared state (card, PIN, accountType, ndcTrace) across all operations in a scenario
- Each operation class is independently reusable — scenario services stitch them together
- `SimulatedHostGateway.balances` is a `ConcurrentHashMap<String, BigDecimal>` — reset on server restart
- Logout sends UNSOLICITED sub-class `B` with `CARD_EJECTED` payload

## API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/atm/withdraw` | Single cash withdrawal |
| POST | `/api/atm/scenario/balance-check` | Scenario 1: Login→Balance→Logout |
| POST | `/api/atm/scenario/transfer-and-balance` | Scenario 2: Login→Balance→Transfer→Balance→Logout |
| POST | `/api/atm/scenario/full-transaction` | Scenario 3: Login→Balance→Transfer→Withdraw→Balance→Logout |

## Next Steps / Known TODOs
- Wire `openConnection()` / `closeConnection()` in scenario services for real host mode
- Add 3DES PIN block encryption under TWK in `PinBlockUtil` for production use
- Add unit tests for `PinBlockUtil`, `NdcMessageBuilder`, operation classes, scenario services
- Consider persisting simulated balances across restarts (currently in-memory only)
