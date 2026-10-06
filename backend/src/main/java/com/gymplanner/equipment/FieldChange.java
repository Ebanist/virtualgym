package com.gymplanner.equipment;

/** Zmiana jednego pola w historii sprzętu (wartości tekstowe do prezentacji). */
public record FieldChange(String oldValue, String newValue) {
}
