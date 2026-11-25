package dsp.a2.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusSplitterTest {

    @Test
    void isDeterministicAndReturnsOnly0Or1() {
        for (int y = 1800; y < 2010; y++) {
            int p = CorpusSplitter.partOf("coffee black strong", y);
            assertTrue(p == 0 || p == 1);
            assertEquals(p, CorpusSplitter.partOf("coffee black strong", y), "same row -> same part");
        }
    }

    @Test
    void dependsOnlyOnNgramAndYearNotOnCounts() {
        NGramRecord a = NGramParser.parse("coffee black strong\t1995\t3\t3\t3");
        NGramRecord b = NGramParser.parse("coffee black strong\t1995\t999\t1\t1");
        NGramRecord c = NGramParser.parse("  coffee  black strong \t1995\t1\t1\t1");
        assertEquals(CorpusSplitter.partOf(a), CorpusSplitter.partOf(b));
        assertEquals(CorpusSplitter.partOf(a), CorpusSplitter.partOf(c));
    }

    @Test
    void sameTrigramAppearsInBothPartsAcrossYears() {
        boolean seen0 = false;
        boolean seen1 = false;
        for (int y = 1900; y < 1920; y++) {
            if (CorpusSplitter.partOf("machine learning model", y) == 0) {
                seen0 = true;
            } else {
                seen1 = true;
            }
        }
        assertTrue(seen0 && seen1, "a trigram seen in 20 years must land in both parts");
    }

    @Test
    void splitIsRoughlyBalanced() {
        int part1 = 0;
        int total = 0;
        for (int w = 0; w < 500; w++) {
            for (int y = 1950; y < 1970; y++) {
                part1 += CorpusSplitter.partOf("word" + w + " other" + (w * 7) + " third", y);
                total++;
            }
        }
        double fraction = part1 / (double) total;
        assertTrue(fraction > 0.47 && fraction < 0.53, "fraction in part 1 = " + fraction);
    }

    @Test
    void knownValuesArePinned() {
        // Pinned so that an accidental change of the split rule (which would change every result) fails here.
        assertEquals(CorpusSplitter.partOf("coffee black strong", 1990),
                CorpusSplitter.partOf(NGramParser.parse("coffee black strong\t1990\t1")));
        int[] expected = new int[6];
        for (int i = 0; i < 6; i++) {
            expected[i] = CorpusSplitter.partOf("coffee black strong", 1990 + i);
        }
        // Recomputed independently with java.util.zip.CRC32:
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        for (int i = 0; i < 6; i++) {
            crc.reset();
            crc.update(("coffee black strong\t" + (1990 + i)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertEquals((int) (crc.getValue() & 1), expected[i]);
        }
    }
}
