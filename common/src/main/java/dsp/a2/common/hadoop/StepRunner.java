package dsp.a2.common.hadoop;

import dsp.a2.common.ConfigKeys;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;

/**
 * Runs one MapReduce step and records its statistics – the same way in both implementations.
 *
 * <pre>
 *   if (!StepRunner.run(job, "step1-count", p.layout().statsDir())) return 1;
 * </pre>
 */
public final class StepRunner {

    private StepRunner() {
    }

    /**
     * Submits the job, waits for it (printing progress), then writes {@code <statsDir>/<stepName>.tsv} and
     * prints a one-line summary. Statistics are written even if the job failed (succeeded=false).
     *
     * @param stepName short name such as "step1-count" (used as the TSV file name)
     * @return true if the job succeeded
     */
    public static boolean run(Job job, String stepName, String statsDir) throws Exception {
        long start = System.currentTimeMillis();
        boolean ok = job.waitForCompletion(true);
        long elapsed = System.currentTimeMillis() - start;

        Configuration conf = job.getConfiguration();
        Counters counters;
        try {
            counters = job.getCounters();
        } catch (Exception e) {
            System.err.println("[stats] could not read counters of " + stepName + ": " + e);
            counters = null;
        }
        JobStats stats = JobStats.from(
                conf.get(ConfigKeys.IMPLEMENTATION, "unknown"), stepName,
                JobSetup.localAggregation(conf), ok, job.getNumReduceTasks(), elapsed, counters);
        System.out.println(stats.toHumanReadable());
        try {
            stats.writeTo(conf, statsDir);
        } catch (Exception e) {
            // Statistics must never fail a (paid) run; the numbers are also in the printed summary.
            System.err.println("[stats] could not write statistics of " + stepName + ": " + e);
        }
        return ok;
    }
}
