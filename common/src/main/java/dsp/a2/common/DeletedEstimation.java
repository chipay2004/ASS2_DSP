package dsp.a2.common;

/**
 * The Deleted Estimation formula – shared so that Pairs and Stripes produce bit-identical probabilities.
 *
 * <pre>
 *                        T_r^01 + T_r^10
 *   P_del(w1 w2 w3) = -------------------------
 *                      N * (N_r^0 + N_r^1)
 * </pre>
 * where (all counts are AFTER stop-word filtering):
 * <ul>
 *   <li>{@code N}      – number of trigram INSTANCES in the whole corpus (sum of match_count of kept rows,
 *                        both parts).</li>
 *   <li>{@code N_r^0}  – number of trigram TYPES that occur exactly r times in part 0.</li>
 *   <li>{@code T_r^01} – total number of occurrences, in part 1, of the types counted in N_r^0.</li>
 *   <li>{@code N_r^1}  – number of trigram TYPES that occur exactly r times in part 1.</li>
 *   <li>{@code T_r^10} – total number of occurrences, in part 0, of the types counted in N_r^1.</li>
 * </ul>
 *
 * <p><b>Which r? (agreed interpretation – see docs/SHARED_CONTRACT.md, "open questions")</b><br>
 * For a trigram with count {@code r0} in part 0 and {@code r1} in part 1, both implementations use
 * <pre>
 *   P = (T_{r0}^01 + T_{r1}^10) / (N * (N_{r0}^0 + N_{r1}^1))
 * </pre>
 * i.e. the part-0 statistics are looked up with the trigram's part-0 count and the part-1 statistics
 * with its part-1 count. This is the combination of the two held-out estimates described in
 * Manning &amp; Schütze 6.2.3–6.2.4. The "types" universe is the set of trigrams seen anywhere in the
 * (filtered) corpus, so r0 = 0 or r1 = 0 are valid and well defined (e.g. N_0^0 = number of types seen
 * only in part 1).
 *
 * <p>This class only evaluates the formula. HOW the four statistics and N are computed is the job of
 * each implementation (Pairs / Stripes).
 */
public final class DeletedEstimation {

    private DeletedEstimation() {
    }

    /**
     * @param nr0  N_{r0}^0
     * @param nr1  N_{r1}^1
     * @param tr01 T_{r0}^01
     * @param tr10 T_{r1}^10
     * @param n    N (total instances in the corpus)
     * @return the deleted-estimation probability
     * @throws IllegalArgumentException if an argument is negative, N is 0, or N_r0^0 + N_r1^1 is 0
     */
    public static double probability(long nr0, long nr1, long tr01, long tr10, long n) {
        if (nr0 < 0 || nr1 < 0 || tr01 < 0 || tr10 < 0) {
            throw new IllegalArgumentException(String.format(
                    "negative statistic: Nr0=%d Nr1=%d Tr01=%d Tr10=%d", nr0, nr1, tr01, tr10));
        }
        if (n <= 0) {
            throw new IllegalArgumentException("N must be positive: " + n);
        }
        if (nr0 + nr1 == 0) {
            throw new IllegalArgumentException("Nr0 + Nr1 is 0 – no trigram type has these counts");
        }
        // Computed in double on purpose: N * (Nr0 + Nr1) can exceed Long.MAX_VALUE on the full corpus
        // (N ~ 1e10..1e11 and Nr ~ 1e8). The operation order is fixed so both implementations get
        // exactly the same double for the same inputs.
        double numerator = (double) (tr01 + tr10);
        double denominator = (double) n * (double) (nr0 + nr1);
        return numerator / denominator;
    }
}
