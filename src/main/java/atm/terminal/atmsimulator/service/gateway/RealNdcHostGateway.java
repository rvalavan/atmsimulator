package atm.terminal.atmsimulator.service.gateway;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.protocol.NdcDelimiter;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;

/**
 * Live TCP/IP gateway to an actual NCR ATM host using the NDC protocol.
 * Active when {@code atm.host.simulated=false}.
 *
 * Each method opens a fresh connection, sends the message, waits for the host
 * response, then closes. For a persistent connection across a full transaction
 * session, refactor to manage socket lifecycle at the service level.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "atm.host.simulated", havingValue = "false")
public class RealNdcHostGateway implements AtmHostGateway {

    @Value("${atm.host.address}")
    private String hostAddress;

    @Value("${atm.host.port}")
    private int hostPort;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // ----- connection lifecycle -----

    public void openConnection() throws IOException {
        socket = new Socket(hostAddress, hostPort);
        out    = new PrintWriter(socket.getOutputStream(), true);
        in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        log.info("Connected to NCR host {}:{}", hostAddress, hostPort);
    }

    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            log.info("Disconnected from NCR host");
        } catch (IOException e) {
            log.warn("Error closing host connection", e);
        }
    }

    // ----- AtmHostGateway implementation -----

    @Override
    public NdcMessage connect(NdcMessage readyMessage) {
        return sendAndReceive(readyMessage);
    }

    @Override
    public NdcMessage sendCardData(NdcMessage cardDataMessage) {
        return sendAndReceive(cardDataMessage);
    }

    @Override
    public NdcMessage sendTransaction(NdcMessage transactionMessage) {
        return sendAndReceive(transactionMessage);
    }

    @Override
    public TransactionResult parseAuthorizationResponse(NdcMessage hostResponse,
                                                        BigDecimal requestedAmount,
                                                        AccountType accountType) {
        String raw = hostResponse.getRawMessage();
        if (raw == null || raw.isEmpty()) {
            return TransactionResult.builder()
                    .approved(false)
                    .dispensedAmount(BigDecimal.ZERO)
                    .hostMessage("NO RESPONSE FROM HOST")
                    .build();
        }

        // NDC authorization response: class 4, status field "001" = approved, "002" = declined
        boolean approved = raw.contains("001");
        String authCode  = approved ? extractAuthCode(raw) : null;

        return TransactionResult.builder()
                .approved(approved)
                .authorizationCode(authCode)
                .dispensedAmount(approved ? requestedAmount : BigDecimal.ZERO)
                .hostMessage(approved ? "APPROVED" : "DECLINED")
                .build();
    }

    // ----- helpers -----

    private NdcMessage sendAndReceive(NdcMessage message) {
        try {
            log.debug("TERMINAL->HOST: {}", message.readable());
            out.println(message.getRawMessage());

            String raw = in.readLine();
            log.debug("HOST->TERMINAL: {}", NdcDelimiter.toReadable(raw));

            NdcMessageClass msgClass = (raw != null && !raw.isEmpty())
                    ? NdcMessageClass.fromCode(raw.charAt(0))
                    : NdcMessageClass.HOST_DATA;

            return NdcMessage.builder()
                    .messageClass(msgClass)
                    .direction("HOST->TERMINAL")
                    .rawMessage(raw)
                    .timestamp(Instant.now())
                    .build();

        } catch (IOException e) {
            log.error("Communication error with NCR host", e);
            return NdcMessage.builder()
                    .messageClass(NdcMessageClass.HOST_DATA)
                    .direction("HOST->TERMINAL")
                    .rawMessage("ERROR: " + e.getMessage())
                    .timestamp(Instant.now())
                    .build();
        }
    }

    private String extractAuthCode(String raw) {
        // Auth code is typically in the last field of the host data response
        String[] parts = raw.split(String.valueOf('\u001C'));
        return parts.length > 2 ? parts[parts.length - 1].trim() : null;
    }
}
