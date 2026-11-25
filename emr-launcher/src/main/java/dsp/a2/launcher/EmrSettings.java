package dsp.a2.launcher;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Personal EMR / S3 settings, read from {@code config/emr.properties} (git-ignored; copy it from
 * {@code config/emr.properties.example}). Contains NO credentials: the AWS SDK takes those from the default
 * provider chain (~/.aws/credentials, environment variables, AWS SSO / Academy session).
 */
public final class EmrSettings {

    private final Properties props;

    EmrSettings(Properties props) {
        this.props = props;
    }

    public static EmrSettings load(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("settings file not found: " + file
                    + " (copy config/emr.properties.example to config/emr.properties and edit it)");
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        EmrSettings s = new EmrSettings(p);
        s.require("aws.region", "s3.bucket", "emr.releaseLabel", "emr.instanceType", "emr.instanceCount",
                "emr.serviceRole", "emr.jobFlowRole", "input.default");
        s.instanceCount(); // validates the number
        return s;
    }

    private void require(String... keys) {
        List<String> missing = new ArrayList<>();
        for (String k : keys) {
            if (get(k) == null) {
                missing.add(k);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("missing settings: " + missing);
        }
    }

    /** Problems that make a real submission impossible (placeholder values etc.). Empty = OK. */
    public List<String> problemsForSubmit() {
        List<String> problems = new ArrayList<>();
        String bucket = bucket();
        if (bucket.contains("CHANGE") || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            problems.add("s3.bucket is not a valid bucket name: '" + bucket + "'");
        }
        return problems;
    }

    /** Trimmed value, or null if absent/blank. */
    String get(String key) {
        String v = props.getProperty(key);
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    public String region() {
        return get("aws.region");
    }

    public String bucket() {
        return get("s3.bucket");
    }

    public String releaseLabel() {
        return get("emr.releaseLabel");
    }

    public String instanceType() {
        return get("emr.instanceType");
    }

    public int instanceCount() {
        try {
            int n = Integer.parseInt(get("emr.instanceCount"));
            if (n < 1) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("emr.instanceCount must be a positive integer");
        }
    }

    /** Optional (null = no SSH key). */
    public String ec2KeyName() {
        return get("emr.ec2KeyName");
    }

    /** Optional (null = let EMR choose). */
    public String availabilityZone() {
        return get("emr.availabilityZone");
    }

    public String serviceRole() {
        return get("emr.serviceRole");
    }

    public String jobFlowRole() {
        return get("emr.jobFlowRole");
    }

    public String defaultInput() {
        return get("input.default");
    }

    /** Main class of an implementation's job jar; defaults to the agreed driver names. */
    public String mainClass(String implementation) {
        String v = get(implementation + ".mainClass");
        if (v != null) {
            return v;
        }
        return "pairs".equals(implementation) ? "dsp.a2.pairs.PairsDriver" : "dsp.a2.stripes.StripesDriver";
    }
}
