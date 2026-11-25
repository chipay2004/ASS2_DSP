package dsp.a2.common;

/**
 * The single entry point for turning a raw dataset line into a usable record. Both implementations
 * call it in their FIRST mapper so that exactly the same rows are kept and the same part is chosen.
 *
 * <p>Order of checks (each dropped row is classified by the FIRST rule it fails):
 * <ol>
 *   <li>{@link Status#MALFORMED} – the line cannot be parsed ({@link NGramParser}).</li>
 *   <li>{@link Status#WRONG_ARITY} – the n-gram does not have exactly {@code n} words.</li>
 *   <li>{@link Status#STOP_WORD} – at least one word is a stop word ({@link StopWords}).</li>
 *   <li>{@link Status#KEPT} – the row is used; its corpus part is computed by {@link CorpusSplitter}.</li>
 * </ol>
 * Only KEPT rows contribute to any count, including N.
 */
public final class CorpusFilter {

    /** Why a line was kept or dropped. */
    public enum Status { KEPT, MALFORMED, WRONG_ARITY, STOP_WORD }

    /** The result of {@link #apply(String)}. {@code record()} and {@code part()} are valid only when KEPT. */
    public static final class Outcome {
        private final Status status;
        private final NGramRecord record;
        private final int part;

        private Outcome(Status status, NGramRecord record, int part) {
            this.status = status;
            this.record = record;
            this.part = part;
        }

        public Status status() {
            return status;
        }

        public boolean isKept() {
            return status == Status.KEPT;
        }

        /** The parsed record (null unless KEPT). */
        public NGramRecord record() {
            return record;
        }

        /** 0 or 1 when KEPT, -1 otherwise. */
        public int part() {
            return part;
        }
    }

    public static final int TRIGRAM = 3;

    private final StopWords stopWords;
    private final int n;

    public CorpusFilter(StopWords stopWords, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive: " + n);
        }
        this.stopWords = stopWords;
        this.n = n;
    }

    /** The filter used by the assignment: trigrams + the given stop words. */
    public static CorpusFilter forTrigrams(StopWords stopWords) {
        return new CorpusFilter(stopWords, TRIGRAM);
    }

    public Outcome apply(String line) {
        NGramRecord record = NGramParser.parse(line);
        if (record == null) {
            return new Outcome(Status.MALFORMED, null, -1);
        }
        if (record.size() != n) {
            return new Outcome(Status.WRONG_ARITY, null, -1);
        }
        if (stopWords.containsStopWord(record)) {
            return new Outcome(Status.STOP_WORD, null, -1);
        }
        return new Outcome(Status.KEPT, record, CorpusSplitter.partOf(record));
    }
}
