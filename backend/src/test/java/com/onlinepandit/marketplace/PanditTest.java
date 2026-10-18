package com.onlinepandit.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PanditTest {
    @Test
    void recordReviewUsesFirstRatingAndIncrementsCount() {
        Pandit pandit = newPandit();

        pandit.recordReview(4);

        assertEquals(new BigDecimal("4.0"), pandit.getRating());
        assertEquals(1, pandit.getReviewCount());
    }

    @Test
    void recordReviewRoundsAverageHalfUpToOneDecimalPlace() {
        Pandit pandit = newPandit();
        pandit.recordReview(4);
        pandit.recordReview(4);
        pandit.recordReview(4);

        pandit.recordReview(5);

        assertEquals(new BigDecimal("4.3"), pandit.getRating());
        assertEquals(4, pandit.getReviewCount());
    }

    private static Pandit newPandit() {
        return new Pandit("Test Pandit", 0, null, "City", null, "Country", "Hindu", null, null, null);
    }
}