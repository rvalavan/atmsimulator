package atm.terminal.atmsimulator.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NdcMessageClassTest {

    @Test
    void code_unsolicited_returns1() {
        assertEquals('1', NdcMessageClass.UNSOLICITED.code());
    }

    @Test
    void code_solicited_returns2() {
        assertEquals('2', NdcMessageClass.SOLICITED.code());
    }

    @Test
    void code_hostCommand_returns3() {
        assertEquals('3', NdcMessageClass.HOST_COMMAND.code());
    }

    @Test
    void code_hostData_returns4() {
        assertEquals('4', NdcMessageClass.HOST_DATA.code());
    }

    @Test
    void fromCode_1_returnsUnsolicited() {
        assertEquals(NdcMessageClass.UNSOLICITED, NdcMessageClass.fromCode('1'));
    }

    @Test
    void fromCode_2_returnsSolicited() {
        assertEquals(NdcMessageClass.SOLICITED, NdcMessageClass.fromCode('2'));
    }

    @Test
    void fromCode_3_returnsHostCommand() {
        assertEquals(NdcMessageClass.HOST_COMMAND, NdcMessageClass.fromCode('3'));
    }

    @Test
    void fromCode_4_returnsHostData() {
        assertEquals(NdcMessageClass.HOST_DATA, NdcMessageClass.fromCode('4'));
    }

    @Test
    void fromCode_unknownCode_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NdcMessageClass.fromCode('9'));
    }

    @Test
    void fromCode_zeroCode_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NdcMessageClass.fromCode('0'));
    }

    @Test
    void roundTrip_codeAndFromCode_areConsistent() {
        for (NdcMessageClass mc : NdcMessageClass.values()) {
            assertEquals(mc, NdcMessageClass.fromCode(mc.code()));
        }
    }
}
