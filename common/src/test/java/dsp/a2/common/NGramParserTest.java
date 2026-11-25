package dsp.a2.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class NGramParserTest {

    @Test
    void parsesFullDatasetLine() {
        NGramRecord r = NGramParser.parse("coffee black strong\t1995\t12\t11\t9");
        assertNotNull(r);
        assertArrayEquals(new String[]{"coffee", "black", "strong"}, r.words());
        assertEquals(3, r.size());
        assertEquals(1995, r.year());
        assertEquals(12L, r.matchCount());
        assertEquals("coffee black strong", r.ngramText());
    }

    @Test
    void acceptsLineWithoutPageAndVolumeCounts() {
        NGramRecord r = NGramParser.parse("a b c\t2000\t7");
        assertNotNull(r);
        assertEquals(7L, r.matchCount());
    }

    @Test
    void collapsesRepeatedAndSurroundingSpaces() {
        NGramRecord r = NGramParser.parse("  machine   learning  model \t2009\t3\t3\t3");
        assertNotNull(r);
        assertEquals("machine learning model", r.ngramText());
    }

    @Test
    void keepsCaseAndUnicode() {
        NGramRecord r = NGramParser.parse("Café crème brûlée\t1999\t2\t2\t2");
        assertNotNull(r);
        assertEquals("Café", r.word(0));
        assertEquals("brûlée", r.word(2));
    }

    @Test
    void returnsCountLargerThanInt() {
        NGramRecord r = NGramParser.parse("x y z\t1999\t5000000000\t1\t1");
        assertNotNull(r);
        assertEquals(5_000_000_000L, r.matchCount());
    }

    @Test
    void doesNotCheckArity() {
        assertEquals(2, NGramParser.parse("coffee black\t1990\t5\t5\t5").size());
        assertEquals(4, NGramParser.parse("a b c d\t1990\t5\t5\t5").size());
    }

    @Test
    void rejectsMalformedLines() {
        assertNull(NGramParser.parse(null));
        assertNull(NGramParser.parse(""));
        assertNull(NGramParser.parse("   "));
        assertNull(NGramParser.parse("coffee black hot"));
        assertNull(NGramParser.parse("coffee black hot\t1990"));
        assertNull(NGramParser.parse("coffee black hot\tabc\t5\t1\t1"));
        assertNull(NGramParser.parse("coffee black hot\t1990\tzero\t1\t1"));
        assertNull(NGramParser.parse("coffee black hot\t1990\t-3\t1\t1"));
        assertNull(NGramParser.parse("coffee black hot\t1990\t0\t1\t1"));
        assertNull(NGramParser.parse("\t1990\t5\t1\t1"));
        assertNull(NGramParser.parse("   \t1990\t5\t1\t1"));
    }
}
