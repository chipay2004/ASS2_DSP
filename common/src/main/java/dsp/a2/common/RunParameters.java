package dsp.a2.common;

import java.util.Locale;

/**
 * The command line shared by both drivers (PairsDriver / StripesDriver), so that both are run with the
 * same parameters locally and on EMR:
 *
 * <pre>
 *   &lt;input&gt; &lt;outputBase&gt; --local-aggregation true|false
 *           [--input-format sequence|text] [--reducers N] [--stopwords PATH]
 * </pre>
 * <ul>
 *   <li>{@code input} – file, directory, glob or comma-separated list (local path, hdfs:// or s3://).</li>
 *   <li>{@code outputBase} – must not exist yet; layout is defined by {@link OutputLayout}.</li>
 *   <li>{@code --local-aggregation} – REQUIRED, so a run is never accidentally measured in the wrong mode.</li>
 *   <li>{@code --input-format} – {@code sequence} (default; the real dataset on S3) or {@code text}
 *       (local test files). Both deliver the same line as the mapper's {@code Text} value.</li>
 *   <li>{@code --reducers} – number of reduce tasks for steps that do not force their own value;
 *       omitted = Hadoop/EMR default.</li>
 *   <li>{@code --stopwords} – optional Hadoop path to an alternative stop-word file.</li>
 * </ul>
 * Generic Hadoop options ({@code -D key=value}) are handled by {@code ToolRunner} before these args.
 */
public final class RunParameters {

    public enum InputFormat {
        SEQUENCE, TEXT;

        public String configValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static InputFormat fromString(String s) {
            for (InputFormat f : values()) {
                if (f.configValue().equalsIgnoreCase(s.trim())) {
                    return f;
                }
            }
            throw new IllegalArgumentException("input format must be 'sequence' or 'text': " + s);
        }
    }

    public static final int DEFAULT_REDUCERS = -1;

    private final String input;
    private final String outputBase;
    private final boolean localAggregation;
    private final InputFormat inputFormat;
    private final int reducers;
    private final String stopWordsPath;

    public RunParameters(String input, String outputBase, boolean localAggregation,
                         InputFormat inputFormat, int reducers, String stopWordsPath) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("input must not be empty");
        }
        if (outputBase == null || outputBase.trim().isEmpty()) {
            throw new IllegalArgumentException("outputBase must not be empty");
        }
        if (reducers != DEFAULT_REDUCERS && reducers < 1) {
            throw new IllegalArgumentException("--reducers must be >= 1: " + reducers);
        }
        this.input = input.trim();
        this.outputBase = outputBase.trim();
        this.localAggregation = localAggregation;
        this.inputFormat = inputFormat;
        this.reducers = reducers;
        this.stopWordsPath = stopWordsPath;
    }

    public static String usage(String program) {
        return "Usage: " + program + " <input> <outputBase> --local-aggregation true|false"
                + " [--input-format sequence|text] [--reducers N] [--stopwords PATH]";
    }

    /**
     * Parses the arguments left after {@code ToolRunner} removed the generic Hadoop options.
     *
     * @throws IllegalArgumentException with a human-readable message on any error
     */
    public static RunParameters parse(String[] args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("missing <input> and/or <outputBase>");
        }
        String input = args[0];
        String output = args[1];
        if (input.startsWith("--") || output.startsWith("--")) {
            throw new IllegalArgumentException("the first two arguments must be <input> <outputBase>");
        }
        Boolean localAggregation = null;
        InputFormat format = InputFormat.SEQUENCE;
        int reducers = DEFAULT_REDUCERS;
        String stopWords = null;

        for (int i = 2; i < args.length; i++) {
            String option = args[i];
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("missing value for " + option);
            }
            String value = args[++i];
            switch (option) {
                case "--local-aggregation":
                    localAggregation = parseBoolean(value);
                    break;
                case "--input-format":
                    format = InputFormat.fromString(value);
                    break;
                case "--reducers":
                    try {
                        reducers = Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("--reducers must be an integer: " + value);
                    }
                    break;
                case "--stopwords":
                    stopWords = value;
                    break;
                default:
                    throw new IllegalArgumentException("unknown option: " + option);
            }
        }
        if (localAggregation == null) {
            throw new IllegalArgumentException("--local-aggregation true|false is required");
        }
        return new RunParameters(input, output, localAggregation, format, reducers, stopWords);
    }

    private static boolean parseBoolean(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.equals("true")) {
            return true;
        }
        if (v.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException("--local-aggregation must be 'true' or 'false': " + s);
    }

    public String input() {
        return input;
    }

    public String outputBase() {
        return outputBase;
    }

    public OutputLayout layout() {
        return new OutputLayout(outputBase);
    }

    public boolean localAggregation() {
        return localAggregation;
    }

    public InputFormat inputFormat() {
        return inputFormat;
    }

    /** {@link #DEFAULT_REDUCERS} when not given. */
    public int reducers() {
        return reducers;
    }

    public boolean hasReducers() {
        return reducers != DEFAULT_REDUCERS;
    }

    /** May be null (use the packaged list). */
    public String stopWordsPath() {
        return stopWordsPath;
    }

    @Override
    public String toString() {
        return "RunParameters{input=" + input + ", outputBase=" + outputBase
                + ", localAggregation=" + localAggregation + ", inputFormat=" + inputFormat.configValue()
                + ", reducers=" + (hasReducers() ? String.valueOf(reducers) : "default")
                + ", stopWords=" + (stopWordsPath == null ? "packaged" : stopWordsPath) + '}';
    }
}
