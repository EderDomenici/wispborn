package dev.eder.wispborn.host;

public enum ConsumeResult {
    OK,
    NOT_HOST,
    NOT_READY,
    /** O dano de consumo foi recusado (mob invulnerável); nada morre. */
    BLOCKED
}
