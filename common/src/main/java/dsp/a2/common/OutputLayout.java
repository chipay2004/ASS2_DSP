package dsp.a2.common;

/**
 * Directory layout UNDER the output base path that a driver receives as its second argument.
 * Works for local paths and for S3 URIs alike (pure string handling).
 *
 * <pre>
 *   &lt;outputBase&gt;/step1-&lt;name&gt;/   intermediate output of MapReduce step 1 (implementation-specific format)
 *   &lt;outputBase&gt;/step2-&lt;name&gt;/   ...
 *   &lt;outputBase&gt;/final/           the knowledge base (contract format, globally sorted part files)
 *   &lt;outputBase&gt;/_stats/          one TSV file per step with the Hadoop counters (see JobStats)
 * </pre>
 * {@code _stats} starts with '_' so Hadoop's FileInputFormat ignores it if a later step reads the base dir.
 */
public final class OutputLayout {

    public static final String FINAL_DIR = "final";
    public static final String STATS_DIR = "_stats";

    private final String base;

    public OutputLayout(String outputBase) {
        if (outputBase == null || outputBase.trim().isEmpty()) {
            throw new IllegalArgumentException("output base must not be empty");
        }
        String b = outputBase.trim();
        while (b.endsWith("/") && b.length() > 1 && !b.endsWith("://")) {
            b = b.substring(0, b.length() - 1);
        }
        this.base = b;
    }

    public String base() {
        return base;
    }

    /**
     * @param index 1-based step number
     * @param name  short lowercase name, e.g. "count" → "&lt;base&gt;/step1-count"
     */
    public String stepDir(int index, String name) {
        if (index < 1) {
            throw new IllegalArgumentException("step index is 1-based: " + index);
        }
        if (name == null || !name.matches("[a-z0-9][a-z0-9-]*")) {
            throw new IllegalArgumentException("step name must match [a-z0-9][a-z0-9-]*: " + name);
        }
        return base + "/step" + index + "-" + name;
    }

    public String finalDir() {
        return base + "/" + FINAL_DIR;
    }

    public String statsDir() {
        return base + "/" + STATS_DIR;
    }
}
