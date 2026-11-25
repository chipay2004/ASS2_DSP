package dsp.a2.common;

import java.util.regex.Pattern;

/**
 * Parses one line (the {@code Text} value) of the Google Books N-gram dataset.
 *
 * <p>Expected format (TAB separated):
 * <pre>
 *   ngram TAB year TAB match_count [TAB page_count TAB volume_count]
 * </pre>
 * <ul>
 *   <li>{@code ngram}: words separated by one or more spaces (leading/trailing spaces are ignored).</li>
 *   <li>{@code year}: integer.</li>
 *   <li>{@code match_count}: positive long.</li>
 *   <li>Any further fields are ignored (the real dataset has page_count and volume_count).</li>
 * </ul>
 *
 * <p>On EMR the dataset is a SequenceFile of {@code (LongWritable rowNumber, Text line)}; locally we use
 * text files with exactly the same line format, read by {@code TextInputFormat}. In both cases the mapper
 * receives the line as its value and must ignore the key.
 *
 * <p>The parser never throws on bad input: it returns {@code null} for a malformed line so that a single
 * corrupt row cannot fail a multi-hour EMR job. The caller counts such rows (see {@link CorpusFilter}).
 * The parser does NOT check the number of words and does NOT change letter case.
 */
public final class NGramParser {

    /** Compiled once: this runs for every one of the hundreds of millions of rows. */
    private static final Pattern SPACES = Pattern.compile(" +");

    private NGramParser() {
    }

    /**
     * @return the parsed record, or {@code null} if the line is malformed.
     */
    public static NGramRecord parse(String line) {
        if (line == null) {
            return null;
        }
        String[] fields = line.split("\t", -1);
        if (fields.length < 3) {
            return null;
        }
        String ngram = fields[0].trim();
        if (ngram.isEmpty()) {
            return null;
        }
        String[] words = SPACES.split(ngram);
        int year;
        long count;
        try {
            year = Integer.parseInt(fields[1].trim());
            count = Long.parseLong(fields[2].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (count <= 0) {
            return null;
        }
        return new NGramRecord(words, year, count);
    }
}
