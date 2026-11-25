package dsp.a2.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputLineTest {

    @Test
    void formatIsWordsSpaceSeparatedThenTabThenScientificProbability() {
        assertEquals("coffee black strong\t1.2500000000e-03",
                OutputLine.format("coffee", "black", "strong", 0.00125));
        assertEquals("1.0000000000e+00", OutputLine.formatProbability(1.0));
    }

    @Test
    void formattingIgnoresDefaultLocale() {
        java.util.Locale old = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY); // would print "1,25" with a default-locale format
            assertEquals("1.2500000000e-03", OutputLine.formatProbability(0.00125));
        } finally {
            java.util.Locale.setDefault(old);
        }
    }

    @Test
    void parseRoundTrip() {
        OutputLine l = OutputLine.parse("new york city\t3.1415926536e-07");
        assertEquals("new", l.w1());
        assertEquals("york", l.w2());
        assertEquals("city", l.w3());
        assertEquals("new york", l.context());
        assertEquals(3.1415926536e-07, l.probability(), 0.0);
        assertEquals("new york city\t3.1415926536e-07", l.format());
    }

    @Test
    void parseRejectsBadLines() {
        assertThrows(IllegalArgumentException.class, () -> OutputLine.parse("new york city 0.5"));
        assertThrows(IllegalArgumentException.class, () -> OutputLine.parse("new york\t0.5"));
        assertThrows(IllegalArgumentException.class, () -> OutputLine.parse("new  york city\t0.5"));
        assertThrows(IllegalArgumentException.class, () -> OutputLine.parse("new york city\tabc"));
        assertThrows(IllegalArgumentException.class, () -> OutputLine.parse("new york city\t0.5\t1"));
    }

    @Test
    void orderIsPairAscendingThenProbabilityDescendingThenW3() {
        List<OutputLine> expected = Arrays.asList(
                new OutputLine("coffee", "black", "strong", 0.6),
                new OutputLine("coffee", "black", "hot", 0.3),
                new OutputLine("coffee", "black", "tasty", 0.3),   // tie on p -> w3 ascending
                new OutputLine("coffee", "instant", "elite", 0.9),
                new OutputLine("desk", "work", "green", 0.1));
        List<OutputLine> shuffled = new ArrayList<>(expected);
        Collections.shuffle(shuffled, new java.util.Random(42));
        shuffled.sort(OutputLine.ORDER);
        assertEquals(expected, shuffled);
    }

    @Test
    void wordOrderIsUtf8ByteOrderLikeHadoopText() {
        // Upper case sorts before lower case; "café" < "coffee" ('a' < 'o'); non-ASCII after ASCII.
        assertTrue(OutputLine.compareUtf8("Coffee", "coffee") < 0);
        assertTrue(OutputLine.compareUtf8("café", "coffee") < 0);
        assertTrue(OutputLine.compareUtf8("zebra", "émigré") < 0);
        assertTrue(OutputLine.compareUtf8("ab", "abc") < 0);
        assertEquals(0, OutputLine.compareUtf8("same", "same"));
        // Supplementary character vs. U+FFFD: String.compareTo would say the opposite.
        String supplementary = new String(Character.toChars(0x1F600));
        assertTrue(OutputLine.compareUtf8("�", supplementary) < 0);
    }
}
