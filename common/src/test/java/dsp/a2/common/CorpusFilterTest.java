package dsp.a2.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusFilterTest {

    private final CorpusFilter filter = CorpusFilter.forTrigrams(new StopWords(Arrays.asList("the", "of", "very")));

    @Test
    void keepsValidTrigramAndAssignsPart() {
        CorpusFilter.Outcome o = filter.apply("coffee black strong\t1990\t3\t3\t3");
        assertEquals(CorpusFilter.Status.KEPT, o.status());
        assertTrue(o.isKept());
        assertEquals("coffee black strong", o.record().ngramText());
        assertEquals(CorpusSplitter.partOf("coffee black strong", 1990), o.part());
    }

    @Test
    void classifiesDroppedLines() {
        assertEquals(CorpusFilter.Status.MALFORMED, filter.apply("").status());
        assertEquals(CorpusFilter.Status.MALFORMED, filter.apply("a b c\tx\t1").status());
        assertEquals(CorpusFilter.Status.WRONG_ARITY, filter.apply("coffee black\t1990\t5").status());
        assertEquals(CorpusFilter.Status.STOP_WORD, filter.apply("the coffee black\t1990\t5").status());
        assertEquals(CorpusFilter.Status.STOP_WORD, filter.apply("coffee black The\t1990\t5").status());
    }

    @Test
    void arityIsCheckedBeforeStopWords() {
        assertEquals(CorpusFilter.Status.WRONG_ARITY, filter.apply("coffee black very hot\t1990\t5").status());
    }

    @Test
    void droppedOutcomeHasNoRecordAndNoPart() {
        CorpusFilter.Outcome o = filter.apply("the coffee black\t1990\t5");
        assertNull(o.record());
        assertEquals(-1, o.part());
    }

    @Test
    void rejectsNonPositiveN() {
        assertThrows(IllegalArgumentException.class, () -> new CorpusFilter(StopWords.empty(), 0));
    }
}
