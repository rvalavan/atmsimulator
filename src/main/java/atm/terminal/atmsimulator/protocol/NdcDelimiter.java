package atm.terminal.atmsimulator.protocol;

/**
 * NDC (NCR Direct Connect) field delimiter characters.
 * These are standard ASCII control characters used to separate
 * fields, groups, records, and units within NDC messages.
 */
public final class NdcDelimiter {

    /** Field Separator — separates top-level message fields. */
    public static final char FS = '\u001C';

    /** Group Separator — separates groups within a field. */
    public static final char GS = '\u001D';

    /** Record Separator — separates records within a group. */
    public static final char RS = '\u001E';

    /** Unit Separator — separates units within a record. */
    public static final char US = '\u001F';

    private NdcDelimiter() {}

    /**
     * Replaces NDC control-character delimiters in a raw message string with
     * human-readable tokens so the message is safe to write to logs or display
     * in a UI without invisible/garbled characters.
     *
     * <pre>
     * \u001C → &lt;FS&gt;
     * \u001D → &lt;GS&gt;
     * \u001E → &lt;RS&gt;
     * \u001F → &lt;US&gt;
     * </pre>
     */
    public static String toReadable(String raw) {
        if (raw == null) return null;
        return raw
                .replace(String.valueOf(FS), "<FS>")
                .replace(String.valueOf(GS), "<GS>")
                .replace(String.valueOf(RS), "<RS>")
                .replace(String.valueOf(US), "<US>");
    }
}
