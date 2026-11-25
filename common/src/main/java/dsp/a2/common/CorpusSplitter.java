package dsp.a2.common;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Splits the corpus into the two parts (0 and 1) required by Deleted Estimation.
 *
 * <p><b>Unit of splitting: one dataset row</b> = one (n-gram, year) pair together with all of its
 * occurrences in that year. A row goes entirely to part 0 or entirely to part 1.
 *
 * <p><b>Decision:</b> {@code part = CRC32(UTF-8 bytes of "w1 w2 w3" + TAB + year) mod 2}.
 *
 * <p>Why this design:
 * <ul>
 *   <li><b>Deterministic and reproducible</b> – it depends only on the row content, not on the input
 *       split, the mapper, the number of reducers, the run, the JVM or the input format (text vs.
 *       SequenceFile). Pairs and Stripes therefore see exactly the same two parts.</li>
 *   <li><b>The same trigram can (and usually does) appear in both parts</b> – its rows for different
 *       years hash independently, so a trigram that appears in k different years lands in both parts
 *       with probability 1 - 2^(1-k). Splitting by the n-gram alone would put every trigram in only
 *       one part and make the held-out counts T_r meaningless.</li>
 *   <li><b>Stateless, O(1) memory</b> – no counters or random state, so it is safe under the Pairs
 *       "no memory assumptions" rule and under task retries / speculative execution.</li>
 *   <li>CRC32 is in the JDK and easy to explain; its lowest bit is close to uniform (~50/50).</li>
 * </ul>
 * A row that appears only once (a trigram seen in a single year) necessarily belongs to a single part;
 * this is expected and is handled by r = 0 in the formula (see {@link DeletedEstimation}).
 */
public final class CorpusSplitter {

    public static final int PART_0 = 0;
    public static final int PART_1 = 1;

    private CorpusSplitter() {
    }

    /** @return {@link #PART_0} or {@link #PART_1} for this dataset row. */
    public static int partOf(NGramRecord record) {
        return partOf(record.ngramText(), record.year());
    }

    /**
     * @param ngramText the words joined by single spaces ("w1 w2 w3")
     * @param year      the row's year
     */
    public static int partOf(String ngramText, int year) {
        CRC32 crc = new CRC32();
        crc.update((ngramText + '\t' + year).getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() & 1L);
    }
}
