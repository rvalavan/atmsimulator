package atm.terminal.atmsimulator.domain;

public enum AccountType {
    CHECKING,
    SAVINGS,
    CREDIT;

    /**
     * NDC account type code sent in transaction request messages.
     * Two-digit format: first digit = from-account type, second digit = to-account type.
     * For withdrawals the to-account is always 0 (none).
     *   1x = Checking, 2x = Savings, 3x = Credit
     */
    public String ndcCode() {
        return switch (this) {
            case CHECKING -> "10";
            case SAVINGS  -> "20";
            case CREDIT   -> "30";
        };
    }
}
