package dsp.a2.launcher;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the SDK-free parts of the launcher: settings, S3 layout and command line. */
class LauncherConfigTest {

    private static Path exampleConfig() {
        return Paths.get(System.getProperty("repo.root", "..")).toAbsolutePath().normalize()
                .resolve("config/emr.properties.example");
    }

    private static EmrSettings settingsWithBucket(String bucket) throws Exception {
        String content = new String(Files.readAllBytes(exampleConfig()), StandardCharsets.UTF_8)
                .replace("CHANGE-ME-your-bucket-name", bucket);
        Path tmp = Files.createTempFile("emr", ".properties");
        Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
        return EmrSettings.load(tmp);
    }

    @Test
    void exampleConfigLoadsButIsNotSubmittable() throws Exception {
        EmrSettings s = EmrSettings.load(exampleConfig());
        assertEquals("us-east-1", s.region());
        assertEquals("emr-7.12.0", s.releaseLabel());
        assertEquals(3, s.instanceCount());
        assertEquals("dsp.a2.pairs.PairsDriver", s.mainClass("pairs"));
        assertEquals("dsp.a2.stripes.StripesDriver", s.mainClass("stripes"));
        assertEquals(null, s.ec2KeyName());
        assertFalse(s.problemsForSubmit().isEmpty(), "placeholder bucket must block --submit");
    }

    @Test
    void realBucketIsSubmittable() throws Exception {
        assertTrue(settingsWithBucket("dsp-a2-alice").problemsForSubmit().isEmpty());
        assertFalse(settingsWithBucket("Bad_Bucket").problemsForSubmit().isEmpty());
    }

    @Test
    void missingFileOrKeysAreReported() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> EmrSettings.load(Paths.get("no/such/file")));
        Path tmp = Files.createTempFile("emr", ".properties");
        Files.write(tmp, Arrays.asList("aws.region=us-east-1"), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> EmrSettings.load(tmp));
    }

    @Test
    void s3LayoutSeparatesPartnersAndRuns() {
        S3Layout a = new S3Layout("b", "pairs", "20261201-1830-la-full");
        S3Layout b = new S3Layout("b", "stripes", "20261201-1830-la-full");
        assertEquals("s3://b/pairs/20261201-1830-la-full/jar/pairs-job.jar", a.jarUri());
        assertEquals("s3://b/pairs/20261201-1830-la-full/output", a.outputBase());
        assertEquals("s3://b/pairs/20261201-1830-la-full/logs/", a.logUri());
        assertFalse(a.outputBase().equals(b.outputBase()));
        assertThrows(IllegalArgumentException.class, () -> new S3Layout("b", "both", "x"));
        assertThrows(IllegalArgumentException.class, () -> new S3Layout("b", "pairs", "Bad Id"));
    }

    @Test
    void commandLineBuildsTheSharedDriverArguments() throws Exception {
        EmrSettings s = settingsWithBucket("dsp-a2-alice");
        LaunchRequest r = LaunchRequest.parse(new String[]{
                "--impl", "pairs", "--run-id", "test-1", "--local-aggregation", "false", "--reducers", "8"});
        assertFalse(r.submit());
        S3Layout l = new S3Layout(s.bucket(), r.implementation(), r.runId());
        List<String> args = r.stepArgs(l, s);
        assertEquals(Arrays.asList(s.defaultInput(), "s3://dsp-a2-alice/pairs/test-1/output",
                "--local-aggregation", "false", "--input-format", "sequence", "--reducers", "8"), args);
        assertEquals(3, r.instanceCount(s));
    }

    @Test
    void commandLineOverridesAndValidation() throws Exception {
        LaunchRequest r = LaunchRequest.parse(new String[]{"--impl", "STRIPES", "--run-id", "x", "--local-aggregation",
                "true", "--input", "s3://b/small.txt", "--input-format", "text", "--instance-count", "2", "--submit"});
        assertTrue(r.submit());
        assertEquals("stripes", r.implementation());
        EmrSettings s = settingsWithBucket("dsp-a2-bob");
        assertEquals(2, r.instanceCount(s));
        assertEquals("s3://b/small.txt", r.stepArgs(new S3Layout("dsp-a2-bob", "stripes", "x"), s).get(0));

        assertThrows(IllegalArgumentException.class, () -> LaunchRequest.parse(new String[]{"--impl", "pairs"}));
        assertThrows(IllegalArgumentException.class, () -> LaunchRequest.parse(new String[]{
                "--impl", "both", "--run-id", "x", "--local-aggregation", "true"}));
        assertThrows(IllegalArgumentException.class, () -> LaunchRequest.parse(new String[]{
                "--impl", "pairs", "--run-id", "x", "--local-aggregation", "yes"}));
        assertThrows(IllegalArgumentException.class, () -> LaunchRequest.parse(new String[]{
                "--impl", "pairs", "--run-id", "x", "--local-aggregation", "true", "--reducers", "0"}));
    }
}
