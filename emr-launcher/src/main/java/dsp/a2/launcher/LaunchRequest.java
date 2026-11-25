package dsp.a2.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command line of the launcher:
 * <pre>
 *   java -jar emr-launcher.jar --impl pairs|stripes --run-id ID --local-aggregation true|false
 *        [--config config/emr.properties] [--input S3_URI] [--input-format sequence|text]
 *        [--reducers N] [--instance-count N] [--submit]
 * </pre>
 * Without {@code --submit} nothing is created on AWS (dry run).
 */
public final class LaunchRequest {

    private String configPath = "config/emr.properties";
    private String implementation;
    private String runId;
    private Boolean localAggregation;
    private String input;
    private String inputFormat = "sequence";
    private Integer reducers;
    private Integer instanceCount;
    private boolean submit;

    private LaunchRequest() {
    }

    public static String usage() {
        return "Usage: java -jar emr-launcher.jar --impl pairs|stripes --run-id ID --local-aggregation true|false\n"
                + "         [--config config/emr.properties] [--input S3_URI] [--input-format sequence|text]\n"
                + "         [--reducers N] [--instance-count N] [--submit]\n"
                + "Without --submit this is a DRY RUN: the request is printed and nothing is created on AWS.";
    }

    public static LaunchRequest parse(String[] args) {
        LaunchRequest r = new LaunchRequest();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--submit".equals(a)) {
                r.submit = true;
                continue;
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("missing value for " + a);
            }
            String v = args[++i].trim();
            switch (a) {
                case "--config":
                    r.configPath = v;
                    break;
                case "--impl":
                    r.implementation = v.toLowerCase(Locale.ROOT);
                    break;
                case "--run-id":
                    r.runId = v;
                    break;
                case "--local-aggregation":
                    if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("--local-aggregation must be true or false");
                    }
                    r.localAggregation = Boolean.parseBoolean(v);
                    break;
                case "--input":
                    r.input = v;
                    break;
                case "--input-format":
                    if (!v.equals("sequence") && !v.equals("text")) {
                        throw new IllegalArgumentException("--input-format must be sequence or text");
                    }
                    r.inputFormat = v;
                    break;
                case "--reducers":
                    r.reducers = positive(a, v);
                    break;
                case "--instance-count":
                    r.instanceCount = positive(a, v);
                    break;
                default:
                    throw new IllegalArgumentException("unknown option " + a);
            }
        }
        if (r.implementation == null || r.runId == null || r.localAggregation == null) {
            throw new IllegalArgumentException("--impl, --run-id and --local-aggregation are required");
        }
        if (!r.implementation.equals("pairs") && !r.implementation.equals("stripes")) {
            throw new IllegalArgumentException("--impl must be pairs or stripes");
        }
        return r;
    }

    private static int positive(String option, String v) {
        try {
            int n = Integer.parseInt(v);
            if (n < 1) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " must be a positive integer: " + v);
        }
    }

    /** The arguments passed to the driver's main (the shared RunParameters command line). */
    public List<String> stepArgs(S3Layout layout, EmrSettings settings) {
        List<String> args = new ArrayList<>();
        args.add(input != null ? input : settings.defaultInput());
        args.add(layout.outputBase());
        args.add("--local-aggregation");
        args.add(String.valueOf(localAggregation));
        args.add("--input-format");
        args.add(inputFormat);
        if (reducers != null) {
            args.add("--reducers");
            args.add(String.valueOf(reducers));
        }
        return args;
    }

    public int instanceCount(EmrSettings settings) {
        return instanceCount != null ? instanceCount : settings.instanceCount();
    }

    public String configPath() {
        return configPath;
    }

    public String implementation() {
        return implementation;
    }

    public String runId() {
        return runId;
    }

    public boolean localAggregation() {
        return localAggregation;
    }

    public boolean submit() {
        return submit;
    }
}
