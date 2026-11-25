package dsp.a2.common.hadoop;

import org.apache.hadoop.conf.Configuration;

/**
 * Configuration for running a driver in-process with Hadoop's LocalJobRunner (no cluster, no Hadoop
 * installation): used by the acceptance tests of both implementations and for quick local runs.
 */
public final class LocalHadoop {

    private LocalHadoop() {
    }

    public static Configuration conf() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file:///");
        conf.set("mapreduce.framework.name", "local");
        // LocalJobRunner supports several reduce tasks; let up to 2 run in parallel. Tests should set
        // job.setNumReduceTasks(>1) so partitioning / global-order bugs show up before EMR.
        conf.setInt("mapreduce.local.reduce.tasks.maximum", 2);
        return conf;
    }
}
