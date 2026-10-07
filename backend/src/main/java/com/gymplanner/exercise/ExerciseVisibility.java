package com.gymplanner.exercise;

/** Widoczność ćwiczenia własnego (CUSTOM). Ćwiczenia z biblioteki (GLOBAL) widzą wszyscy. */
public enum ExerciseVisibility {
    /** Tylko autor. */
    PRIVATE,
    /** Wszyscy członkowie siłowni, w której dodano ćwiczenie. */
    GYM
}
