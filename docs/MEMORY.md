# ATM Simulator — Project Memory

## Quick Reference
- **Repo**: https://github.com/rvalavan/atmsimulator
- **Local path**: `c:\smart-brain\app\claude\ATMSimulator`
- **Stack**: Spring Boot 4.1.0-SNAPSHOT, Java 21, Maven, Lombok
- **Default run**: `./mvnw spring-boot:run` → http://localhost:8080
- **API**: `POST /api/atm/withdraw`
- **Details**: See [architecture.md](architecture.md)

## Current Status (as of 2026-03-08)
- Withdrawal flow fully implemented and tested via Postman ✓
- Simulated host mode working (approval ≤ $500, decline > $500) ✓
- NDC message format corrected to match production sample ✓
- README on GitHub includes build/run/test docs + real host connection guide ✓

## Key Decisions Made
- Sub-class `T` (not `E`) for transaction request messages — confirmed from production NDC sample
- Account type is 2-digit NDC code: CHECKING=`10`, SAVINGS=`20`, CREDIT=`30`
- PIN block: ISO 9564 Format 0, clear-text in simulation (needs 3DES encryption for real host)
- `AtmHostGateway` interface enables zero-code-change swap between simulated and real host
- `NdcMessage.readable()` / `NdcDelimiter.toReadable()` used in all logs and JSON responses — never log `rawMessage` directly

## Next Steps / Known TODOs
- Wire `openConnection()` / `closeConnection()` in `AtmSimulationService` for real host mode
- Add 3DES PIN block encryption under TWK in `PinBlockUtil` for production use
- Add Balance Inquiry and Transfer operations (controller + service + NDC messages)
- Add unit tests for `PinBlockUtil`, `NdcMessageBuilder`, `AtmSimulationService`
