package atm.terminal.atmsimulator.domain;

import atm.terminal.atmsimulator.protocol.NdcMessage;
import atm.terminal.atmsimulator.protocol.NdcMessageClass;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AtmSessionTest {

    private NdcMessage msg(String sub) {
        return NdcMessage.builder()
                .messageClass(NdcMessageClass.UNSOLICITED)
                .messageSubClass(sub)
                .direction("TERMINAL->HOST")
                .rawMessage("raw")
                .timestamp(Instant.now())
                .build();
    }

    @Test
    void builder_defaultState_isIdle() {
        AtmSession session = AtmSession.builder()
                .cardNumber("4111111111111111")
                .build();
        assertEquals(TerminalState.IDLE, session.getState());
    }

    @Test
    void builder_defaultNdcTrace_isEmpty() {
        AtmSession session = AtmSession.builder()
                .cardNumber("4111111111111111")
                .build();
        assertNotNull(session.getNdcTrace());
        assertTrue(session.getNdcTrace().isEmpty());
    }

    @Test
    void addMessage_appendsOneMessage() {
        AtmSession session = AtmSession.builder().cardNumber("4111111111111111").build();
        NdcMessage m = msg("F");
        session.addMessage(m);
        assertEquals(1, session.getNdcTrace().size());
        assertSame(m, session.getNdcTrace().get(0));
    }

    @Test
    void addMessages_appendsAllMessages() {
        AtmSession session = AtmSession.builder().cardNumber("4111111111111111").build();
        NdcMessage m1 = msg("F");
        NdcMessage m2 = msg("T");
        NdcMessage m3 = msg("B");
        session.addMessages(List.of(m1, m2, m3));
        assertEquals(3, session.getNdcTrace().size());
    }

    @Test
    void addMessages_multipleCalls_accumulatesAll() {
        AtmSession session = AtmSession.builder().cardNumber("4111111111111111").build();
        session.addMessages(List.of(msg("F"), msg("E")));
        session.addMessages(List.of(msg("T"), msg("A")));
        assertEquals(4, session.getNdcTrace().size());
    }

    @Test
    void setState_updatesState() {
        AtmSession session = AtmSession.builder().cardNumber("4111111111111111").build();
        session.setState(TerminalState.PIN_ENTRY);
        assertEquals(TerminalState.PIN_ENTRY, session.getState());
    }

    @Test
    void builder_setsAllFields() {
        AtmSession session = AtmSession.builder()
                .cardNumber("4111111111111111")
                .expiryDate("2812")
                .pin("1234")
                .accountType(AccountType.CHECKING)
                .build();
        assertEquals("4111111111111111", session.getCardNumber());
        assertEquals("2812", session.getExpiryDate());
        assertEquals("1234", session.getPin());
        assertEquals(AccountType.CHECKING, session.getAccountType());
    }
}
