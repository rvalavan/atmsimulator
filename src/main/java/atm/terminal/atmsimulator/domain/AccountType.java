package atm.terminal.atmsimulator.domain;

public enum AccountType {
    CHECKING,
    SAVINGS,
    CREDIT;

    /** NDC account type code sent in transaction request messages. */
    public String ndcCode() {
        return switch (this) {
            case CHECKING -> "1";
            case SAVINGS  -> "2";
            case CREDIT   -> "3";
        };
    }
}
