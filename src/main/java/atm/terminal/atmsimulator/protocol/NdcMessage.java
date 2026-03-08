package atm.terminal.atmsimulator.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a single NDC protocol message exchanged between terminal and host.
 * Included in ATM responses to give testers full visibility of the NDC exchange.
 */
@Data
@Builder
public class NdcMessage {

    /** Message class (first character of the raw NDC frame). */
    private NdcMessageClass messageClass;

    /** Message sub-class identifier (e.g. "F" for Ready, "E" for Transaction). */
    private String messageSubClass;

    /** Direction of the message: "TERMINAL->HOST" or "HOST->TERMINAL". */
    private String direction;

    /**
     * Raw NDC message bytes as a Java string. Contains actual control-character
     * delimiters (FS \u001C, GS \u001D, RS \u001E, US \u001F) exactly as they
     * appear on the wire. Jackson serialises these as Unicode escapes in JSON.
     */
    private String rawMessage;

    /** When the message was created or received. */
    private Instant timestamp;

    /**
     * Human-readable form of {@link #rawMessage} with control-character delimiters
     * replaced by printable tokens: &lt;FS&gt;, &lt;GS&gt;, &lt;RS&gt;, &lt;US&gt;.
     * Safe to write to log files or display in a UI without garbled characters.
     * Included in JSON responses as {@code "readableMessage"}.
     */
    @JsonProperty("readableMessage")
    public String readable() {
        return NdcDelimiter.toReadable(rawMessage);
    }
}
