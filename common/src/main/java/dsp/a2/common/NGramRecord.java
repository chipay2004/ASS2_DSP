package dsp.a2.common;

import java.util.Arrays;
import java.util.Objects;

/**
 * One parsed row of the Google Books N-gram dataset (version 20090715).
 *
 * <p>A row describes how many times ONE n-gram appeared in ONE year:
 * <pre>
 *   ngram TAB year TAB match_count TAB page_count TAB volume_count
 *   e.g.  "coffee black strong\t1995\t12\t11\t9"
 * </pre>
 * Only the fields used by the assignment are kept: the words, the year and {@code match_count}
 * (the number of occurrences = "instances"). {@code page_count} and {@code volume_count} are ignored.
 *
 * <p>Immutable and Hadoop-independent. Instances are created by {@link NGramParser}.
 */
public final class NGramRecord {

    private final String[] words;
    private final int year;
    private final long matchCount;

    public NGramRecord(String[] words, int year, long matchCount) {
        Objects.requireNonNull(words, "words");
        if (words.length == 0) {
            throw new IllegalArgumentException("an n-gram must contain at least one word");
        }
        for (String w : words) {
            if (w == null || w.isEmpty()) {
                throw new IllegalArgumentException("empty word in n-gram: " + Arrays.toString(words));
            }
        }
        if (matchCount <= 0) {
            throw new IllegalArgumentException("match count must be positive: " + matchCount);
        }
        this.words = words.clone();
        this.year = year;
        this.matchCount = matchCount;
    }

    /** Number of words (3 for a trigram). */
    public int size() {
        return words.length;
    }

    /** The i-th word (0-based): word(0) = w1, word(1) = w2, word(2) = w3. */
    public String word(int i) {
        return words[i];
    }

    public String[] words() {
        return words.clone();
    }

    /** The n-gram text, words separated by a single space: "w1 w2 w3". */
    public String ngramText() {
        return String.join(" ", words);
    }

    public int year() {
        return year;
    }

    /** Number of occurrences (instances) of this n-gram in this year. */
    public long matchCount() {
        return matchCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NGramRecord)) {
            return false;
        }
        NGramRecord other = (NGramRecord) o;
        return year == other.year && matchCount == other.matchCount && Arrays.equals(words, other.words);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(words) + year) + Long.hashCode(matchCount);
    }

    @Override
    public String toString() {
        return "NGramRecord{" + ngramText() + ", year=" + year + ", count=" + matchCount + '}';
    }
}
