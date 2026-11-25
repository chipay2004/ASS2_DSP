package dsp.a2.common.hadoop;

import dsp.a2.common.CorpusFilter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * Hadoop counters describing the input filtering. Both implementations update them in their first mapper
 * through {@link #record(TaskAttemptContext, CorpusFilter.Outcome)}, so the numbers are directly comparable.
 *
 * <p>{@link #N_INSTANCES} is N of the formula. Counters of failed / speculative task attempts are discarded
 * by Hadoop, so after a successful job the value is exact. A driver reads it with {@link #value} and passes
 * it on to later jobs via {@code ConfigKeys.CORPUS_N} (see {@link JobSetup#setCorpusN}).
 */
public enum CommonCounters {
    /** Every line the first mapper received. */
    INPUT_LINES,
    /** Unparseable lines. */
    MALFORMED,
    /** Lines whose n-gram does not have exactly 3 words. */
    WRONG_ARITY,
    /** Lines dropped because the n-gram contains a stop word. */
    STOP_WORD_FILTERED,
    /** Rows kept (rows, not occurrences). */
    KEPT_ROWS,
    KEPT_ROWS_PART0,
    KEPT_ROWS_PART1,
    /** N: sum of match_count over kept rows (both parts). */
    N_INSTANCES,
    /** Sum of match_count over kept rows of part 0. */
    N_INSTANCES_PART0,
    /** Sum of match_count over kept rows of part 1. */
    N_INSTANCES_PART1;

    /** Updates the counters for one input line. Call once per map() invocation of the first mapper. */
    public static void record(TaskAttemptContext context, CorpusFilter.Outcome outcome) {
        context.getCounter(INPUT_LINES).increment(1);
        switch (outcome.status()) {
            case MALFORMED:
                context.getCounter(MALFORMED).increment(1);
                break;
            case WRONG_ARITY:
                context.getCounter(WRONG_ARITY).increment(1);
                break;
            case STOP_WORD:
                context.getCounter(STOP_WORD_FILTERED).increment(1);
                break;
            case KEPT:
                long count = outcome.record().matchCount();
                context.getCounter(KEPT_ROWS).increment(1);
                context.getCounter(N_INSTANCES).increment(count);
                if (outcome.part() == 0) {
                    context.getCounter(KEPT_ROWS_PART0).increment(1);
                    context.getCounter(N_INSTANCES_PART0).increment(count);
                } else {
                    context.getCounter(KEPT_ROWS_PART1).increment(1);
                    context.getCounter(N_INSTANCES_PART1).increment(count);
                }
                break;
            default:
                throw new IllegalStateException("unknown status " + outcome.status());
        }
    }

    /** Reads one of these counters from a finished job's counters. */
    public static long value(Counters counters, CommonCounters counter) {
        return counters.findCounter(counter).getValue();
    }
}
