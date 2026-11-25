package dsp.a2.common;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopWordsTest {

    private static NGramRecord rec(String ngram) {
        return NGramParser.parse(ngram + "\t2000\t1\t1\t1");
    }

    @Test
    void matchingIsCaseInsensitive() {
        StopWords sw = new StopWords(Arrays.asList("the", "OF"));
        assertTrue(sw.isStopWord("the"));
        assertTrue(sw.isStopWord("The"));
        assertTrue(sw.isStopWord("THE"));
        assertTrue(sw.isStopWord("of"));
        assertFalse(sw.isStopWord("theory"));
        assertFalse(sw.isStopWord(null));
    }

    @Test
    void anyPositionRemovesTheNGram() {
        StopWords sw = new StopWords(Arrays.asList("the"));
        assertTrue(sw.containsStopWord(rec("the coffee black")));
        assertTrue(sw.containsStopWord(rec("coffee the black")));
        assertTrue(sw.containsStopWord(rec("coffee black The")));
        assertFalse(sw.containsStopWord(rec("coffee black strong")));
    }

    @Test
    void readerIgnoresCommentsAndBlankLines() throws Exception {
        StopWords sw = StopWords.fromReader(new StringReader("# comment\n\n  the \nOf\n#a\n"));
        assertEquals(2, sw.size());
        assertTrue(sw.isStopWord("of"));
        assertFalse(sw.isStopWord("a"));
        assertFalse(sw.isStopWord("# comment"));
    }

    @Test
    void emptyListFiltersNothing() {
        assertFalse(StopWords.empty().containsStopWord(rec("the of and")));
    }

    @Test
    void defaultListIsPackagedAndMatchesDataFile() throws Exception {
        StopWords packaged = StopWords.loadDefault();
        assertTrue(packaged.size() > 50);
        assertTrue(packaged.isStopWord("the"));
        StopWords file = StopWords.fromReader(
                java.nio.file.Files.newBufferedReader(TestFiles.stopWordsFile()));
        assertEquals(file.size(), packaged.size(), "jar resource must be data/stopwords.txt");
    }
}
