package dsp.a2.launcher;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.emr.EmrClient;
import software.amazon.awssdk.services.emr.model.RunJobFlowRequest;
import software.amazon.awssdk.services.emr.model.RunJobFlowResponse;

import java.nio.file.Paths;
import java.util.List;

/**
 * Submits one run (one implementation, one local-aggregation mode) to Amazon EMR.
 *
 * <p>SAFETY: the default is a DRY RUN that only prints what would be created. A cluster (which costs
 * money) is created only with {@code --submit}. Upload the job jar first (scripts/upload-jar.sh).
 */
public final class EmrLauncher {

    private EmrLauncher() {
    }

    public static void main(String[] args) throws Exception {
        LaunchRequest r;
        EmrSettings s;
        try {
            r = LaunchRequest.parse(args);
            s = EmrSettings.load(Paths.get(r.configPath()));
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println(LaunchRequest.usage());
            System.exit(2);
            return;
        }
        S3Layout layout = new S3Layout(s.bucket(), r.implementation(), r.runId());
        RunJobFlowRequest request = EmrRequestFactory.build(s, r, layout);

        System.out.println("Implementation : " + r.implementation());
        System.out.println("Run id         : " + r.runId());
        System.out.println("Release        : " + s.releaseLabel() + " in " + s.region());
        System.out.println("Instances      : " + r.instanceCount(s) + " x " + s.instanceType());
        System.out.println("Jar            : " + layout.jarUri());
        System.out.println("Main class     : " + s.mainClass(r.implementation()));
        System.out.println("Step args      : " + String.join(" ", r.stepArgs(layout, s)));
        System.out.println("Logs           : " + layout.logUri());

        List<String> problems = s.problemsForSubmit();
        if (!r.submit()) {
            problems.forEach(p -> System.out.println("WARNING: " + p));
            System.out.println("\nDRY RUN - nothing was created on AWS. Add --submit to start the (paid) cluster.");
            System.out.println(request);
            return;
        }
        if (!problems.isEmpty()) {
            problems.forEach(p -> System.err.println("Error: " + p));
            System.exit(2);
        }
        try (EmrClient emr = EmrClient.builder().region(Region.of(s.region())).build()) {
            RunJobFlowResponse response = emr.runJobFlow(request);
            System.out.println("\nStarted cluster " + response.jobFlowId());
            System.out.println("It terminates by itself when the step ends. Check the EMR console, and when done");
            System.out.println("download results with scripts/fetch-results.sh " + r.implementation() + " " + r.runId());
        }
    }
}
