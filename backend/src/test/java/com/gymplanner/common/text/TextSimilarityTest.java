package com.gymplanner.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSimilarityTest {

    @Test
    void identicalAndDisjointTexts() {
        assertThat(TextSimilarity.dice("bieznia", "bieznia")).isEqualTo(1.0);
        assertThat(TextSimilarity.dice("abc", "xyz")).isZero();
    }

    @Test
    void detectsSimilarExerciseNames() {
        String existing = TextNormalizer.normalize("Bieg na bieżni");
        assertThat(TextSimilarity.similar(existing, TextNormalizer.normalize("bieg na biezni szybki"), 0.7)).isTrue();
        assertThat(TextSimilarity.similar(existing, TextNormalizer.normalize("Bieg na bieżni"), 0.7)).isTrue();
        assertThat(TextSimilarity.similar(existing, TextNormalizer.normalize("Chodzenie na bieżni"), 0.7)).isFalse();
        assertThat(TextSimilarity.similar(existing, TextNormalizer.normalize("Martwy ciąg"), 0.7)).isFalse();
    }
}
