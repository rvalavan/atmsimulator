package atm.terminal.atmsimulator.protocol;

/**
 * NDC message class codes.
 * The first character of every NDC message identifies its class.
 */
public enum NdcMessageClass {

    /** Terminal-initiated messages (card data, transaction requests). */
    UNSOLICITED('1'),

    /** Terminal status / ready messages sent in response to host polls. */
    SOLICITED('2'),

    /** Host-initiated control messages (screen commands, state changes). */
    HOST_COMMAND('3'),

    /** Host authorization / data response messages. */
    HOST_DATA('4');

    private final char code;

    NdcMessageClass(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    public static NdcMessageClass fromCode(char code) {
        for (NdcMessageClass mc : values()) {
            if (mc.code == code) return mc;
        }
        throw new IllegalArgumentException("Unknown NDC message class: " + code);
    }
}
