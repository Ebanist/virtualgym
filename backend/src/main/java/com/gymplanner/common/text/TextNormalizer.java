package com.gymplanner.common.text;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalizuje tekst do porównań i wyszukiwania: małe litery, bez polskich znaków diakrytycznych,
 * pojedyncze spacje.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String lower = input.trim().toLowerCase(Locale.ROOT).replace('ł', 'l');
        String withoutDiacritics = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutDiacritics.replaceAll("[^\\p{Alnum}]+", " ").trim();
    }
}
