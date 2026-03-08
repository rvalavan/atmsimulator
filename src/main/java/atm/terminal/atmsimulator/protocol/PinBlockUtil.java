package atm.terminal.atmsimulator.protocol;

/**
 * Builds an ISO 9564 Format 0 PIN block.
 *
 * <pre>
 * Format block : 0 + len(PIN) + PIN digits + padding (F) to 16 hex digits
 * PAN block    : 0000 + rightmost 12 digits of PAN excluding check digit
 * PIN block    : Format block XOR PAN block
 * </pre>
 *
 * Example — PIN "1234", PAN "4111111111111111":
 * <pre>
 *   Format block = 041234FFFFFFFFFF
 *   PAN block    = 0000111111111111
 *   PIN block    = 041225EEEEEEEEEE
 * </pre>
 *
 * In a production system the PIN block would be encrypted under the terminal's
 * working key (TWK) using 3DES before transmission. This utility produces the
 * clear-text PIN block only, which is sufficient for simulator testing.
 */
public final class PinBlockUtil {

    private PinBlockUtil() {}

    public static String buildPinBlock(String pin, String pan) {
        String formatBlock = buildFormatBlock(pin);
        String panBlock    = buildPanBlock(pan);
        return xorHex(formatBlock, panBlock);
    }

    private static String buildFormatBlock(String pin) {
        StringBuilder sb = new StringBuilder();
        sb.append('0');
        sb.append(Integer.toHexString(pin.length()).toUpperCase());
        sb.append(pin);
        while (sb.length() < 16) sb.append('F');
        return sb.toString();
    }

    private static String buildPanBlock(String pan) {
        // Strip check digit (last digit), take the rightmost 12 of what remains
        String withoutCheck = pan.substring(0, pan.length() - 1);
        String rightmost12  = withoutCheck.substring(withoutCheck.length() - 12);
        return "0000" + rightmost12;
    }

    private static String xorHex(String a, String b) {
        StringBuilder result = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            int xored = Character.digit(a.charAt(i), 16) ^ Character.digit(b.charAt(i), 16);
            result.append(Integer.toHexString(xored).toUpperCase());
        }
        return result.toString();
    }
}
