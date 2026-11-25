package dsp.a2.common.tools;

import dsp.a2.common.OutputLine;
import dsp.a2.common.StopWords;
import dsp.a2.common.TestFiles;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceEstimatorTest {

    /**
     * The assignment's 2-gram example.
     * Part 0: ילד מותר ילד אסור ילד מותר          Part 1: ילד אסור ילד מוזר ילד אסור ילד מותר
     */
    private static Map<String, long[]> hebrewExample() {
        Map<String, long[]> c = new LinkedHashMap<>();
        c.put("ילד מותר", new long[]{2, 1});
        c.put("מותר ילד", new long[]{1, 0});
        c.put("ילד אסור", new long[]{1, 2});
        c.put("אסור ילד", new long[]{1, 2});
        c.put("ילד מוזר", new long[]{0, 1});
        c.put("מוזר ילד", new long[]{0, 1});
        return c;
    }

    @Test
    void reproducesTheStatisticsOfTheAssignmentExample() {
        Map<Long, ReferenceEstimator.RStats> t = ReferenceEstimator.buildTable(hebrewExample());
        assertEquals(3, t.get(1L).nr0);   // N_1^0 = 3
        assertEquals(4, t.get(1L).tr01);  // T_1^01 = 4
        assertEquals(3, t.get(1L).nr1);   // N_1^1 = 3
        assertEquals(2, t.get(1L).tr10);  // T_1^10 = 2
        // r = 0 is well defined over the types seen in the corpus:
        assertEquals(2, t.get(0L).nr0);   // types seen only in part 1
        assertEquals(2, t.get(0L).tr01);
        assertEquals(1, t.get(0L).nr1);   // types seen only in part 0
        assertEquals(1, t.get(0L).tr10);
    }

    @Test
    void probabilitiesOfTheAssignmentExample() {
        Map<String, Double> p = ReferenceEstimator.estimate(hebrewExample(), 12);
        assertEquals(3.0 / 48.0, p.get("ילד מותר"), 1e-15);   // (T_2^01 + T_1^10) / (12 (N_2^0 + N_1^1)) = 3/48
        assertEquals(5.0 / 48.0, p.get("מותר ילד"), 1e-15);   // (4 + 1) / (12 (3 + 1))
        assertEquals(6.0 / 60.0, p.get("ילד אסור"), 1e-15);   // (4 + 2) / (12 (3 + 2))
        assertEquals(6.0 / 60.0, p.get("אסור ילד"), 1e-15);
        assertEquals(4.0 / 60.0, p.get("ילד מוזר"), 1e-15);   // (2 + 2) / (12 (2 + 3))
        assertEquals(4.0 / 60.0, p.get("מוזר ילד"), 1e-15);
    }

    @Test
    void ignoresDroppedLinesAndProducesSortedOutput() {
        List<String> lines = Arrays.asList(
                "a b c\t2000\t2\t1\t1",
                "a b c\t2001\t3\t1\t1",
                "a b d\t2000\t1\t1\t1",
                "the b c\t2000\t100\t1\t1",
                "malformed",
                "a b\t2000\t50\t1\t1");
        List<OutputLine> out = ReferenceEstimator.fromLines(lines, new StopWords(Arrays.asList("the")));
        assertEquals(2, out.size());
        assertEquals("a b c", out.get(0).trigram());
        assertTrue(out.get(0).probability() >= out.get(1).probability());
    }

    @Test
    void emptyInputGivesEmptyOutput() {
        assertTrue(ReferenceEstimator.fromLines(new ArrayList<>(), StopWords.empty()).isEmpty());
    }

    /**
     * Golden test: data/expected/test-input.expected.tsv must be exactly what the shared code produces for
     * data/test-input.txt with data/stopwords.txt. If you change the stop-word list or the test input,
     * regenerate it with scripts/make-expected.sh.
     */
    @Test
    void goldenFileIsUpToDate() throws Exception {
        List<String> input = Files.readAllLines(TestFiles.testInput(), StandardCharsets.UTF_8);
        List<OutputLine> actual = ReferenceEstimator.fromLines(input, StopWords.loadDefault());
        List<String> actualLines = new ArrayList<>();
        for (OutputLine l : actual) {
            actualLines.add(l.format());
        }
        List<String> expected = Files.readAllLines(TestFiles.expectedOutput(), StandardCharsets.UTF_8);
        assertEquals(expected, actualLines, "regenerate with scripts/make-expected.sh");
        assertFalse(expected.isEmpty());
    }
}
