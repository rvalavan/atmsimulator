package atm.terminal.atmsimulator.domain;

public enum TerminalState {
    IDLE,
    CARD_READ,
    PIN_ENTRY,
    TRANSACTION_PROCESSING,
    DISPENSING,
    RECEIPT_PRINTING,
    CARD_EJECTED,
    ERROR
}
