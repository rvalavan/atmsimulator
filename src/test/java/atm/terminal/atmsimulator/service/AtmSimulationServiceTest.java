package atm.terminal.atmsimulator.service;

import atm.terminal.atmsimulator.domain.AccountType;
import atm.terminal.atmsimulator.domain.TransactionResult;
import atm.terminal.atmsimulator.model.request.AtmRequest;
import atm.terminal.atmsimulator.model.response.AtmResponse;
import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import atm.terminal.atmsimulator.service.gateway.AtmHostGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtmSimulationServiceTest {

    @Mock
    private NdcMessageBuilder messageBuilder;

    @Mock
    private AtmHostGateway hostGateway;

    @InjectMocks
    private AtmSimulationService service;

    private NdcMessage msg(NdcMessageClass cls, String sub, String dir) {
        return NdcMessage.builder()
                .messageClass(cls).messageSubClass(sub).direction(dir)
                .rawMessage("test").timestamp(Instant.now()).build();
    }

    private AtmRequest buildRequest(BigDecimal amount) {
        AtmRequest req = new AtmRequest();
        req.setCardNumber("4111111111111111");
        req.setExpiryDate("2812");
        req.setPin("1234");
        req.setAmount(amount);
        req.setAccountType(AccountType.CHECKING);
        return req;
    }

    private void setupMocksForWithdrawal(boolean approved) {
        when(messageBuilder.buildSolicitedReady())
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "TERMINAL->HOST"));
        when(messageBuilder.buildCardDataMessage(any(), any()))
                .thenReturn(msg(NdcMessageClass.UNSOLICITED, "E", "TERMINAL->HOST"));
        when(messageBuilder.buildTransactionRequest(any(), any(), any(), any()))
                .thenReturn(msg(NdcMessageClass.UNSOLICITED, "T", "TERMINAL->HOST"));

        when(hostGateway.connect(any()))
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "HOST->TERMINAL"));
        when(hostGateway.sendCardData(any()))
                .thenReturn(msg(NdcMessageClass.HOST_COMMAND, "8", "HOST->TERMINAL"));
        when(hostGateway.sendTransaction(any()))
                .thenReturn(msg(NdcMessageClass.HOST_DATA, "A", "HOST->TERMINAL"));

        TransactionResult result = approved
                ? TransactionResult.builder().approved(true).authorizationCode("ABC123")
                        .dispensedAmount(BigDecimal.valueOf(100)).hostMessage("APPROVED").build()
                : TransactionResult.builder().approved(false)
                        .dispensedAmount(BigDecimal.ZERO).hostMessage("DECLINED - EXCEEDS LIMIT").build();

        when(hostGateway.parseAuthorizationResponse(any(), any(), any(), any())).thenReturn(result);
    }

    @Test
    void processWithdrawal_approved_returnsSuccess() {
        setupMocksForWithdrawal(true);
        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));
        assertTrue(response.isSuccess());
        assertEquals("APPROVED", response.getStatus());
    }

    @Test
    void processWithdrawal_approved_authCodePopulated() {
        setupMocksForWithdrawal(true);
        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));
        assertEquals("ABC123", response.getAuthorizationCode());
    }

    @Test
    void processWithdrawal_approved_dispensedAmountPopulated() {
        setupMocksForWithdrawal(true);
        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));
        assertEquals(BigDecimal.valueOf(100), response.getDispensedAmount());
    }

    @Test
    void processWithdrawal_approved_ndcTraceHas6Messages() {
        setupMocksForWithdrawal(true);
        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));
        assertEquals(6, response.getNdcTrace().size());
    }

    @Test
    void processWithdrawal_declined_returnsDeclined() {
        setupMocksForWithdrawal(false);
        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(600)));
        assertFalse(response.isSuccess());
        assertEquals("DECLINED", response.getStatus());
    }

    @Test
    void processWithdrawal_declined_messageContainsDeclined() {
        setupMocksForWithdrawal(false);
        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(600)));
        assertTrue(response.getMessage().contains("DECLINED"));
    }

    @Test
    void processWithdrawal_exceptionDuringConnect_returnsError() {
        when(messageBuilder.buildSolicitedReady())
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "TERMINAL->HOST"));
        when(hostGateway.connect(any())).thenThrow(new RuntimeException("Connection refused"));

        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));
        assertFalse(response.isSuccess());
        assertEquals("ERROR", response.getStatus());
        assertEquals("Connection refused", response.getMessage());
    }

    @Test
    void processWithdrawal_ndcTraceIsNeverNull() {
        when(messageBuilder.buildSolicitedReady())
                .thenReturn(msg(NdcMessageClass.SOLICITED, "F", "TERMINAL->HOST"));
        when(hostGateway.connect(any())).thenThrow(new RuntimeException("fail"));

        AtmResponse response = service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));
        assertNotNull(response.getNdcTrace());
    }

    @Test
    void processWithdrawal_allFourGatewayMethodsCalled() {
        setupMocksForWithdrawal(true);
        service.processWithdrawal(buildRequest(BigDecimal.valueOf(100)));

        verify(hostGateway).connect(any());
        verify(hostGateway).sendCardData(any());
        verify(hostGateway).sendTransaction(any());
        verify(hostGateway).parseAuthorizationResponse(any(), any(), any(), any());
    }
}
