package dsp.a2.common;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * One line of the FINAL output (the knowledge base), identical for Pairs and Stripes.
 *
 * <p><b>Format</b> (UTF-8, one trigram per line):
 * <pre>
 *   w1 SPACE w2 SPACE w3 TAB probability
 *   e.g.  "coffee black strong\t1.2345678900e-03"
 * </pre>
 * The probability is written with {@link #formatProbability(double)} ({@code %.10e}, Locale.ROOT), so
 * both implementations print the same characters for the same double. With Hadoop's
 * {@code TextOutputFormat} this is simply key = {@code "w1 w2 w3"}, value = formatted probability.
 *
 * <p><b>Order</b> ({@link #ORDER}):
 * <ol>
 *   <li>w1 ascending, then w2 ascending (byte-wise UTF-8 order, the same order as Hadoop {@code Text});</li>
 *   <li>probability descending;</li>
 *   <li>tie-break (our choice, not specified by the assignment): w3 ascending, so output is deterministic.</li>
 * </ol>
 * Concatenating the final part files {@code part-r-00000, part-r-00001, ...} in file-name order must give
 * a list sorted by this order.
 */
public final class OutputLine implements Comparable<OutputLine> {

    /** The contract order of the final output. */
    public static final Comparator<OutputLine> ORDER = OutputLine::compareTo;

    private final String w1;
    private final String w2;
    private final String w3;
    private final double probability;

    public OutputLine(String w1, String w2, String w3, double probability) {
        this.w1 = Objects.requireNonNull(w1);
        this.w2 = Objects.requireNonNull(w2);
        this.w3 = Objects.requireNonNull(w3);
        this.probability = probability;
    }

    public String w1() {
        return w1;
    }

    public String w2() {
        return w2;
    }

    public String w3() {
        return w3;
    }

    public double probability() {
        return probability;
    }

    /** "w1 w2 w3" */
    public String trigram() {
        return w1 + ' ' + w2 + ' ' + w3;
    }

    /** "w1 w2" – the context (the word pair) used for grouping and for the analysis report. */
    public String context() {
        return w1 + ' ' + w2;
    }

    /** The exact output line, without a trailing newline. */
    public String format() {
        return format(w1, w2, w3, probability);
    }

    public static String format(String w1, String w2, String w3, double probability) {
        return w1 + ' ' + w2 + ' ' + w3 + '\t' + formatProbability(probability);
    }

    /** The single probability formatting used everywhere: e.g. 1.2345678900e-03. */
    public static String formatProbability(double probability) {
        return String.format(Locale.ROOT, "%.10e", probability);
    }

    /**
     * Parses a line produced by {@link #format()}.
     *
     * @throws IllegalArgumentException if the line does not follow the output format
     */
    public static OutputLine parse(String line) {
        int tab = line.indexOf('\t');
        if (tab < 0 || line.indexOf('\t', tab + 1) >= 0) {
            throw new IllegalArgumentException("expected exactly one TAB: '" + line + "'");
        }
        String[] words = line.substring(0, tab).split(" ", -1);
        if (words.length != 3 || words[0].isEmpty() || words[1].isEmpty() || words[2].isEmpty()) {
            throw new IllegalArgumentException("expected 'w1 w2 w3' before the TAB: '" + line + "'");
        }
        double p;
        try {
            p = Double.parseDouble(line.substring(tab + 1).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad probability: '" + line + "'", e);
        }
        return new OutputLine(words[0], words[1], words[2], p);
    }

    @Override
    public int compareTo(OutputLine o) {
        return compare(w1, w2, w3, probability, o.w1, o.w2, o.w3, o.probability);
    }

    /** The contract order expressed on raw fields (used also by the Hadoop sort key). */
    public static int compare(String aW1, String aW2, String aW3, double aP,
                              String bW1, String bW2, String bW3, double bP) {
        int c = compareUtf8(aW1, bW1);
        if (c != 0) {
            return c;
        }
        c = compareUtf8(aW2, bW2);
        if (c != 0) {
            return c;
        }
        c = Double.compare(bP, aP); // descending probability
        if (c != 0) {
            return c;
        }
        return compareUtf8(aW3, bW3);
    }

    /** Unsigned byte-wise comparison of the UTF-8 encodings (= Hadoop Text order = code-point order). */
    public static int compareUtf8(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(x.length, y.length);
        for (int i = 0; i < len; i++) {
            int c = (x[i] & 0xff) - (y[i] & 0xff);
            if (c != 0) {
                return c;
            }
        }
        return x.length - y.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutputLine)) {
            return false;
        }
        OutputLine other = (OutputLine) o;
        return Double.compare(probability, other.probability) == 0
                && w1.equals(other.w1) && w2.equals(other.w2) && w3.equals(other.w3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(w1, w2, w3, probability);
    }

    @Override
    public String toString() {
        return format();
    }
}
