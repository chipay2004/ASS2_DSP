package dsp.a2.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeletedEstimationTest {

    @Test
    void assignmentExampleValues() {
        // Assignment example (bigrams): N = 12, N_1^0 = 3, T_1^01 = 4, N_1^1 = 3, T_1^10 = 2.
        // A bigram with r0 = 1 and r1 = 1 would get (4 + 2) / (12 * (3 + 3)) = 6 / 72.
        assertEquals(6.0 / 72.0, DeletedEstimation.probability(3, 3, 4, 2, 12), 1e-15);
    }

    @Test
    void simpleValues() {
        assertEquals(0.5, DeletedEstimation.probability(1, 1, 1, 1, 2), 0.0);
        assertEquals(0.0, DeletedEstimation.probability(2, 0, 0, 0, 10), 0.0);
    }

    @Test
    void doesNotOverflowOnFullCorpusMagnitudes() {
        long n = 80_000_000_000L;        // ~ 8e10 instances
        long nr0 = 300_000_000L;         // ~ 3e8 types -> N * (Nr0 + Nr1) ~ 4.8e19 > Long.MAX_VALUE
        long nr1 = 300_000_000L;
        double p = DeletedEstimation.probability(nr0, nr1, 400_000_000L, 400_000_000L, n);
        assertTrue(p > 0 && p < 1e-10, "p = " + p);
        assertEquals(800_000_000.0 / (8e10 * 6e8), p, 1e-25);
    }

    @Test
    void sameInputsGiveBitIdenticalResult() {
        double a = DeletedEstimation.probability(123456, 654321, 999999, 111111, 987654321L);
        double b = DeletedEstimation.probability(123456, 654321, 999999, 111111, 987654321L);
        assertEquals(Double.doubleToLongBits(a), Double.doubleToLongBits(b));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> DeletedEstimation.probability(1, 1, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> DeletedEstimation.probability(0, 0, 1, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> DeletedEstimation.probability(-1, 1, 1, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> DeletedEstimation.probability(1, 1, -1, 1, 5));
    }
}
