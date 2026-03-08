package atm.terminal.atmsimulator.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NdcDelimiterTest {

    @Test
    void toReadable_replacesFieldSeparator() {
        String input = "2" + NdcDelimiter.FS + "F" + NdcDelimiter.FS + "00000001";
        String result = NdcDelimiter.toReadable(input);
        assertEquals("2<FS>F<FS>00000001", result);
    }

    @Test
    void toReadable_replacesGroupSeparator() {
        String input = "data" + NdcDelimiter.GS + "group";
        String result = NdcDelimiter.toReadable(input);
        assertEquals("data<GS>group", result);
    }

    @Test
    void toReadable_replacesRecordSeparator() {
        String input = "rec1" + NdcDelimiter.RS + "rec2";
        String result = NdcDelimiter.toReadable(input);
        assertEquals("rec1<RS>rec2", result);
    }

    @Test
    void toReadable_replacesUnitSeparator() {
        String input = "unit1" + NdcDelimiter.US + "unit2";
        String result = NdcDelimiter.toReadable(input);
        assertEquals("unit1<US>unit2", result);
    }

    @Test
    void toReadable_replacesAllDelimitersInOneString() {
        String input = "1" + NdcDelimiter.FS + "T" + NdcDelimiter.FS + "00000001"
                + NdcDelimiter.FS + "0001" + NdcDelimiter.GS + "020000"
                + NdcDelimiter.GS + "000000010000";
        String result = NdcDelimiter.toReadable(input);
        assertTrue(result.contains("<FS>"));
        assertTrue(result.contains("<GS>"));
        assertFalse(result.contains(String.valueOf(NdcDelimiter.FS)));
        assertFalse(result.contains(String.valueOf(NdcDelimiter.GS)));
    }

    @Test
    void toReadable_noDelimiters_returnsUnchanged() {
        String input = "plain text without delimiters";
        assertEquals(input, NdcDelimiter.toReadable(input));
    }

    @Test
    void toReadable_emptyString_returnsEmpty() {
        assertEquals("", NdcDelimiter.toReadable(""));
    }

    @Test
    void toReadable_null_returnsNull() {
        assertNull(NdcDelimiter.toReadable(null));
    }

    @Test
    void constants_haveCorrectValues() {
        assertEquals('\u001C', NdcDelimiter.FS);
        assertEquals('\u001D', NdcDelimiter.GS);
        assertEquals('\u001E', NdcDelimiter.RS);
        assertEquals('\u001F', NdcDelimiter.US);
    }
}
