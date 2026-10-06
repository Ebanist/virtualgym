package com.gymplanner.user;

/**
 * Role w systemie. W MVP używana jest wyłącznie {@link #USER};
 * {@link #GYM_ADMIN} i {@link #TRAINER} są przygotowane pod wersję płatną.
 */
public enum Role {
    USER,
    GYM_ADMIN,
    TRAINER
}
