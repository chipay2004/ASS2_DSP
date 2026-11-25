package dsp.a2.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The stop-word list and the stop-word rule shared by both implementations.
 *
 * <p><b>Rule (see docs/SHARED_CONTRACT.md):</b> an n-gram is removed from ALL counts (including N) if ANY
 * of its words is a stop word. Matching is case-insensitive ({@code "The"} matches {@code "the"}); the
 * n-gram itself is not lower-cased.
 *
 * <p>File format: one word per line, UTF-8; blank lines and lines starting with {@code #} are ignored.
 * The default list is {@code data/stopwords.txt}, packaged into the jar as the classpath resource
 * {@value #DEFAULT_RESOURCE}.
 *
 * <p>Memory: the list has a few hundred words, so holding it in every mapper is O(1) memory and is
 * allowed under the Pairs "no memory assumptions" rule (it does not grow with the corpus).
 */
public final class StopWords {

    public static final String DEFAULT_RESOURCE = "stopwords.txt";

    private final Set<String> words;

    public StopWords(Collection<String> words) {
        Set<String> normalized = new HashSet<>();
        for (String w : words) {
            String n = normalize(w);
            if (!n.isEmpty()) {
                normalized.add(n);
            }
        }
        this.words = Collections.unmodifiableSet(normalized);
    }

    /** An empty list (useful in tests). */
    public static StopWords empty() {
        return new StopWords(Collections.emptySet());
    }

    /** Reads a stop-word file (one word per line, '#' comments). The reader is closed. */
    public static StopWords fromReader(Reader reader) throws IOException {
        Set<String> result = new HashSet<>();
        try (BufferedReader in = new BufferedReader(reader)) {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                result.add(trimmed);
            }
        }
        return new StopWords(result);
    }

    /** Reads a stop-word list from a UTF-8 stream. The stream is closed. */
    public static StopWords fromStream(InputStream in) throws IOException {
        return fromReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Loads the default list packaged in the jar ({@value #DEFAULT_RESOURCE}). */
    public static StopWords loadDefault() {
        InputStream in = StopWords.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE);
        if (in == null) {
            throw new IllegalStateException("Stop-word resource '" + DEFAULT_RESOURCE
                    + "' not found on the classpath. Was the common module built with Maven?");
        }
        try {
            return fromStream(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + DEFAULT_RESOURCE, e);
        }
    }

    public boolean isStopWord(String word) {
        return word != null && words.contains(normalize(word));
    }

    /** True if ANY word of the record is a stop word. */
    public boolean containsStopWord(NGramRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (isStopWord(record.word(i))) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return words.size();
    }

    private static String normalize(String w) {
        return w == null ? "" : w.trim().toLowerCase(Locale.ROOT);
    }
}
