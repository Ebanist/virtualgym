package com.gymplanner.common.text;

import java.util.HashMap;
import java.util.Map;

/** Podobieństwo krótkich tekstów (już znormalizowanych) – współczynnik Dice'a na bigramach znaków. */
public final class TextSimilarity {

    private TextSimilarity() {
    }

    /** 0.0 – brak wspólnych bigramów, 1.0 – identyczne. */
    public static double dice(String a, String b) {
        if (a == null || b == null || a.length() < 2 || b.length() < 2) {
            return a != null && a.equals(b) ? 1.0 : 0.0;
        }
        Map<String, Integer> bigrams = new HashMap<>();
        for (int i = 0; i < a.length() - 1; i++) {
            bigrams.merge(a.substring(i, i + 2), 1, Integer::sum);
        }
        int common = 0;
        for (int i = 0; i < b.length() - 1; i++) {
            String bigram = b.substring(i, i + 2);
            Integer count = bigrams.get(bigram);
            if (count != null && count > 0) {
                common++;
                bigrams.put(bigram, count - 1);
            }
        }
        return 2.0 * common / (a.length() - 1 + b.length() - 1);
    }

    /** Podobne, gdy jedna nazwa zawiera drugą albo współczynnik Dice'a przekracza próg. */
    public static boolean similar(String a, String b, double threshold) {
        return a.contains(b) || b.contains(a) || dice(a, b) >= threshold;
    }
}
