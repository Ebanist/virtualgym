package com.gymplanner.auth.dto;

/** Wspólne reguły hasła – muszą być zgodne ze schematem Zod na froncie. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;
    /** Co najmniej jedna litera i jedna cyfra. */
    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\d).*$";
    public static final String MESSAGE = "must contain at least one letter and one digit";

    private PasswordPolicy() {
    }
}
