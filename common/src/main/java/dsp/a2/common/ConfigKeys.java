package dsp.a2.common;

/**
 * Names of the Hadoop {@code Configuration} keys shared by both implementations.
 * The driver sets them (see {@code JobSetup}); mappers/reducers read them with
 * {@code context.getConfiguration().get(KEY)}.
 */
public final class ConfigKeys {

    private ConfigKeys() {
    }

    /** "pairs" or "stripes" – written into the statistics. */
    public static final String IMPLEMENTATION = "dsp.a2.implementation";

    /** "true"/"false": whether local aggregation (combiner / in-mapper combining) is enabled. */
    public static final String LOCAL_AGGREGATION = "dsp.a2.local.aggregation";

    /** "text" or "sequence" – see {@link RunParameters.InputFormat}. */
    public static final String INPUT_FORMAT = "dsp.a2.input.format";

    /** Optional Hadoop path (local / HDFS / S3) to a stop-word file overriding the packaged list. */
    public static final String STOPWORDS_PATH = "dsp.a2.stopwords.path";

    /**
     * N – total number of trigram instances after filtering. Typically read by the driver from
     * {@code CommonCounters.N_INSTANCES} after the first job and passed to later jobs through this key.
     */
    public static final String CORPUS_N = "dsp.a2.corpus.n";
}
