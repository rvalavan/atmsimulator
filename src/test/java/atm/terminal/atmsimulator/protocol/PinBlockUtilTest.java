package atm.terminal.atmsimulator.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PinBlockUtilTest {

    @Test
    void buildPinBlock_knownExample_from_javadoc() {
        // PIN=1234, PAN=4111111111111111 → documented expected result
        // formatBlock = 041234FFFFFFFFFF
        // panBlock    = 0000111111111111
        // pinBlock    = 041225EEEEEEEEEE
        String result = PinBlockUtil.buildPinBlock("1234", "4111111111111111");
        assertEquals("041225EEEEEEEEEE", result);
    }

    @Test
    void buildPinBlock_resultIs16Characters() {
        String result = PinBlockUtil.buildPinBlock("1234", "4111111111111111");
        assertEquals(16, result.length());
    }

    @Test
    void buildPinBlock_resultIsUpperCase() {
        String result = PinBlockUtil.buildPinBlock("1234", "4111111111111111");
        assertTrue(result.chars().allMatch(c -> !Character.isLetter(c) || Character.isUpperCase(c)));
    }

    @Test
    void buildPinBlock_differentPin_9999() {
        // formatBlock = 049999FFFFFFFFFF
        // panBlock    = 0000111111111111
        // pinBlock    = 049988EEEEEEEEEE
        String result = PinBlockUtil.buildPinBlock("9999", "4111111111111111");
        assertEquals("049988EEEEEEEEEE", result);
    }

    @Test
    void buildPinBlock_allZeroPin() {
        // formatBlock = 040000FFFFFFFFFF
        // panBlock    = 0000111111111111
        // XOR pos-by-pos: 0,4,0,0,1,1,E,E,E,E,E,E,E,E,E,E
        // pinBlock    = 040011EEEEEEEEEE
        String result = PinBlockUtil.buildPinBlock("0000", "4111111111111111");
        assertEquals("040011EEEEEEEEEE", result);
    }

    @Test
    void buildPinBlock_differentPan_panBlockUsesRightmost12ExcludingCheck() {
        // PAN = "5500000000000004" (16 digits)
        // withoutCheck = "550000000000000" (15 digits)
        // rightmost12  = "000000000000"  (positions 3..14)
        // panBlock = "0000000000000000"
        // PIN = "1234" → formatBlock = "041234FFFFFFFFFF"
        // XOR: 041234FFFFFFFFFF XOR 0000000000000000 = 041234FFFFFFFFFF
        String result = PinBlockUtil.buildPinBlock("1234", "5500000000000004");
        assertEquals("041234FFFFFFFFFF", result);
    }

    @Test
    void buildPinBlock_pinAndPanXorIsInvertible() {
        // Building pin block twice with same inputs should give same result (deterministic)
        String r1 = PinBlockUtil.buildPinBlock("5678", "4111111111111111");
        String r2 = PinBlockUtil.buildPinBlock("5678", "4111111111111111");
        assertEquals(r1, r2);
    }
}
