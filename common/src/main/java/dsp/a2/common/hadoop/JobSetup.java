package dsp.a2.common.hadoop;

import dsp.a2.common.ConfigKeys;
import dsp.a2.common.RunParameters;
import dsp.a2.common.StopWords;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import java.io.IOException;
import java.io.InputStream;

/**
 * Hadoop wiring that must be identical in both drivers.
 *
 * <p>Typical use in a driver ({@code run(String[] args)} of a {@code Tool}):
 * <pre>
 *   RunParameters p = RunParameters.parse(args);
 *   Configuration conf = getConf();
 *   JobSetup.apply(conf, p, "pairs");               // BEFORE creating any Job (Job copies the conf)
 *
 *   Job first = Job.getInstance(conf, "pairs-step1-...");
 *   JobSetup.configureCorpusInput(first, p);        // only for the job(s) that read the raw corpus
 *   JobSetup.configureReducers(first, p);
 *   ...
 *   StepRunner.run(first, "step1-...", p.layout().statsDir());
 *   long n = CommonCounters.value(first.getCounters(), CommonCounters.N_INSTANCES);
 *   JobSetup.setCorpusN(conf, n);                   // later jobs read it with JobSetup.corpusN(conf)
 * </pre>
 */
public final class JobSetup {

    /** Hadoop key for map-output compression. Forced off so shuffle-size statistics are comparable. */
    public static final String MAP_OUTPUT_COMPRESS = "mapreduce.map.output.compress";

    private JobSetup() {
    }

    /** Copies the run parameters into the configuration. Call before {@code Job.getInstance(conf)}. */
    public static void apply(Configuration conf, RunParameters p, String implementation) {
        conf.set(ConfigKeys.IMPLEMENTATION, implementation);
        conf.setBoolean(ConfigKeys.LOCAL_AGGREGATION, p.localAggregation());
        conf.set(ConfigKeys.INPUT_FORMAT, p.inputFormat().configValue());
        if (p.stopWordsPath() != null) {
            conf.set(ConfigKeys.STOPWORDS_PATH, p.stopWordsPath());
        }
        // Shared measurement condition (docs/SHARED_CONTRACT.md): map output is not compressed, so
        // MAP_OUTPUT_MATERIALIZED_BYTES reflects the real serialized size in both implementations.
        conf.setBoolean(MAP_OUTPUT_COMPRESS, false);
    }

    /** Sets the input format (text / sequence) and the input path(s) of a job that reads the corpus. */
    public static void configureCorpusInput(Job job, RunParameters p) throws IOException {
        if (p.inputFormat() == RunParameters.InputFormat.SEQUENCE) {
            job.setInputFormatClass(SequenceFileInputFormat.class);
        } else {
            job.setInputFormatClass(TextInputFormat.class);
        }
        FileInputFormat.addInputPaths(job, p.input());
    }

    /** Applies {@code --reducers} if it was given. A step that needs a fixed number may override it. */
    public static void configureReducers(Job job, RunParameters p) {
        if (p.hasReducers()) {
            job.setNumReduceTasks(p.reducers());
        }
    }

    public static boolean localAggregation(Configuration conf) {
        return conf.getBoolean(ConfigKeys.LOCAL_AGGREGATION, false);
    }

    public static void setCorpusN(Configuration conf, long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("N must be positive (did the first job keep any row?): " + n);
        }
        conf.setLong(ConfigKeys.CORPUS_N, n);
    }

    /** @throws IllegalStateException if N was not set by the driver */
    public static long corpusN(Configuration conf) {
        long n = conf.getLong(ConfigKeys.CORPUS_N, -1L);
        if (n <= 0) {
            throw new IllegalStateException(ConfigKeys.CORPUS_N + " is not set in the job configuration");
        }
        return n;
    }

    /**
     * Loads the stop words for a mapper's {@code setup()}: the file at {@code ConfigKeys.STOPWORDS_PATH}
     * if set (any Hadoop file system, e.g. s3://), otherwise the list packaged in the jar.
     */
    public static StopWords loadStopWords(Configuration conf) throws IOException {
        String path = conf.get(ConfigKeys.STOPWORDS_PATH);
        if (path == null || path.trim().isEmpty()) {
            return StopWords.loadDefault();
        }
        Path p = new Path(path.trim());
        FileSystem fs = p.getFileSystem(conf);
        try (InputStream in = fs.open(p)) {
            return StopWords.fromStream(in);
        }
    }
}
