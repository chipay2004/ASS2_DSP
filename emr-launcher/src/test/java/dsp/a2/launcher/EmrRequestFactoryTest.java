package dsp.a2.launcher;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.emr.model.ActionOnFailure;
import software.amazon.awssdk.services.emr.model.RunJobFlowRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Builds the SDK request object only – no network call, no AWS resources. */
class EmrRequestFactoryTest {

    @Test
    void requestAutoTerminatesAndRunsTheSharedCommandLine() throws Exception {
        Path example = Paths.get(System.getProperty("repo.root", "..")).toAbsolutePath().normalize()
                .resolve("config/emr.properties.example");
        Path cfg = Files.createTempFile("emr", ".properties");
        Files.write(cfg, new String(Files.readAllBytes(example), StandardCharsets.UTF_8)
                .replace("CHANGE-ME-your-bucket-name", "dsp-a2-test").getBytes(StandardCharsets.UTF_8));
        EmrSettings s = EmrSettings.load(cfg);
        LaunchRequest r = LaunchRequest.parse(new String[]{
                "--impl", "stripes", "--run-id", "r1", "--local-aggregation", "true"});
        RunJobFlowRequest req = EmrRequestFactory.build(s, r, new S3Layout(s.bucket(), "stripes", "r1"));

        assertEquals("emr-7.12.0", req.releaseLabel());
        assertFalse(req.instances().keepJobFlowAliveWhenNoSteps());
        assertNull(req.instances().ec2KeyName());
        assertEquals(1, req.steps().size());
        assertEquals(ActionOnFailure.TERMINATE_CLUSTER, req.steps().get(0).actionOnFailure());
        assertEquals("s3://dsp-a2-test/stripes/r1/jar/stripes-job.jar", req.steps().get(0).hadoopJarStep().jar());
        assertEquals("dsp.a2.stripes.StripesDriver", req.steps().get(0).hadoopJarStep().mainClass());
        assertEquals("s3://dsp-a2-test/stripes/r1/output", req.steps().get(0).hadoopJarStep().args().get(1));
        assertEquals("s3://dsp-a2-test/stripes/r1/logs/", req.logUri());
    }
}
