package dsp.a2.common.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.TaskCounter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The statistics of ONE MapReduce step, taken from Hadoop's built-in counters (the same numbers that
 * appear in the job log). Written as a small TSV file per step to {@code <outputBase>/_stats/}.
 *
 * <p>How the assignment's required numbers map to counters (identical for Pairs and Stripes –
 * see docs/SHARED_CONTRACT.md):
 * <ul>
 *   <li>key-value records sent from mappers to reducers → {@code REDUCE_INPUT_RECORDS}
 *       (after local aggregation; {@code MAP_OUTPUT_RECORDS} is the count before it);</li>
 *   <li>their size → {@code MAP_OUTPUT_MATERIALIZED_BYTES} (bytes written for the shuffle, after
 *       combining; map-output compression is OFF), plus {@code MAP_OUTPUT_BYTES} (before combining);</li>
 *   <li>number of different keys → {@code REDUCE_INPUT_GROUPS}.</li>
 * </ul>
 */
public final class JobStats {

    /** Column order of the TSV file. Do not reorder: reports and tools rely on it. */
    public static final String[] COLUMNS = {
            "implementation", "step", "local_aggregation", "succeeded", "reduce_tasks", "elapsed_ms",
            "map_input_records", "map_output_records", "map_output_bytes", "map_output_materialized_bytes",
            "combine_input_records", "combine_output_records",
            "reduce_input_groups", "reduce_input_records", "reduce_shuffle_bytes", "reduce_output_records",
            "spilled_records"
    };

    private final String implementation;
    private final String step;
    private final boolean localAggregation;
    private final boolean succeeded;
    private final int reduceTasks;
    private final long elapsedMs;
    private final long[] counterValues;

    private static final TaskCounter[] COUNTERS = {
            TaskCounter.MAP_INPUT_RECORDS, TaskCounter.MAP_OUTPUT_RECORDS, TaskCounter.MAP_OUTPUT_BYTES,
            TaskCounter.MAP_OUTPUT_MATERIALIZED_BYTES,
            TaskCounter.COMBINE_INPUT_RECORDS, TaskCounter.COMBINE_OUTPUT_RECORDS,
            TaskCounter.REDUCE_INPUT_GROUPS, TaskCounter.REDUCE_INPUT_RECORDS, TaskCounter.REDUCE_SHUFFLE_BYTES,
            TaskCounter.REDUCE_OUTPUT_RECORDS, TaskCounter.SPILLED_RECORDS
    };

    private JobStats(String implementation, String step, boolean localAggregation, boolean succeeded,
                     int reduceTasks, long elapsedMs, long[] counterValues) {
        this.implementation = implementation;
        this.step = step;
        this.localAggregation = localAggregation;
        this.succeeded = succeeded;
        this.reduceTasks = reduceTasks;
        this.elapsedMs = elapsedMs;
        this.counterValues = counterValues;
    }

    public static JobStats from(String implementation, String step, boolean localAggregation, boolean succeeded,
                                int reduceTasks, long elapsedMs, Counters counters) {
        long[] values = new long[COUNTERS.length];
        for (int i = 0; i < COUNTERS.length; i++) {
            values[i] = counters == null ? 0L : counters.findCounter(COUNTERS[i]).getValue();
        }
        return new JobStats(implementation, step, localAggregation, succeeded, reduceTasks, elapsedMs, values);
    }

    public long get(TaskCounter counter) {
        for (int i = 0; i < COUNTERS.length; i++) {
            if (COUNTERS[i] == counter) {
                return counterValues[i];
            }
        }
        throw new IllegalArgumentException("counter not collected: " + counter);
    }

    public String implementation() {
        return implementation;
    }

    public String step() {
        return step;
    }

    public boolean localAggregation() {
        return localAggregation;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public static String tsvHeader() {
        return String.join("\t", COLUMNS);
    }

    public String toTsvRow() {
        StringBuilder sb = new StringBuilder();
        sb.append(implementation).append('\t').append(step).append('\t').append(localAggregation).append('\t')
                .append(succeeded).append('\t').append(reduceTasks).append('\t').append(elapsedMs);
        for (long v : counterValues) {
            sb.append('\t').append(v);
        }
        return sb.toString();
    }

    /** Short summary printed to stdout (visible in the EMR step's stdout log). */
    public String toHumanReadable() {
        return String.format(
                "[stats] %s/%s localAggregation=%s ok=%s reducers=%d time=%.1fs | sent-to-reducers=%d records,"
                        + " %d bytes (materialized), distinct keys=%d | map-output(before combine)=%d records, %d bytes",
                implementation, step, localAggregation, succeeded, reduceTasks, elapsedMs / 1000.0,
                get(TaskCounter.REDUCE_INPUT_RECORDS), get(TaskCounter.MAP_OUTPUT_MATERIALIZED_BYTES),
                get(TaskCounter.REDUCE_INPUT_GROUPS), get(TaskCounter.MAP_OUTPUT_RECORDS),
                get(TaskCounter.MAP_OUTPUT_BYTES));
    }

    /** Writes {@code <statsDir>/<step>.tsv} (header + one row), overwriting an older file. */
    public void writeTo(Configuration conf, String statsDir) throws IOException {
        Path file = new Path(statsDir, step + ".tsv");
        FileSystem fs = file.getFileSystem(conf);
        try (FSDataOutputStream out = fs.create(file, true)) {
            out.write((tsvHeader() + "\n" + toTsvRow() + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }
}
