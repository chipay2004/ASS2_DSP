package dsp.a2.launcher;

/**
 * The agreed S3 layout: every partner and every run gets its own prefix, so runs never overwrite each other.
 *
 * <pre>
 *   s3://&lt;bucket&gt;/&lt;impl&gt;/&lt;runId&gt;/jar/&lt;impl&gt;-job.jar   the exact jar that was run
 *   s3://&lt;bucket&gt;/&lt;impl&gt;/&lt;runId&gt;/output/              outputBase passed to the driver
 *                                                        (step dirs, final/, _stats/)
 *   s3://&lt;bucket&gt;/&lt;impl&gt;/&lt;runId&gt;/logs/                EMR logs (step stdout/syslog with counters)
 * </pre>
 * {@code impl} is "pairs" or "stripes"; {@code runId} follows docs/SHARED_CONTRACT.md, e.g.
 * {@code 20261201-1830-la-full}.
 */
public final class S3Layout {

    private final String bucket;
    private final String implementation;
    private final String runId;

    public S3Layout(String bucket, String implementation, String runId) {
        if (!"pairs".equals(implementation) && !"stripes".equals(implementation)) {
            throw new IllegalArgumentException("implementation must be 'pairs' or 'stripes': " + implementation);
        }
        if (runId == null || !runId.matches("[a-z0-9][a-z0-9-]{0,62}")) {
            throw new IllegalArgumentException("runId must match [a-z0-9][a-z0-9-]*: " + runId);
        }
        this.bucket = bucket;
        this.implementation = implementation;
        this.runId = runId;
    }

    public String runPrefix() {
        return "s3://" + bucket + "/" + implementation + "/" + runId;
    }

    public String jarUri() {
        return runPrefix() + "/jar/" + implementation + "-job.jar";
    }

    public String outputBase() {
        return runPrefix() + "/output";
    }

    public String logUri() {
        return runPrefix() + "/logs/";
    }

    public String implementation() {
        return implementation;
    }

    public String runId() {
        return runId;
    }
}
