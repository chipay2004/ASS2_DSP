package dsp.a2.launcher;

import software.amazon.awssdk.services.emr.model.ActionOnFailure;
import software.amazon.awssdk.services.emr.model.Application;
import software.amazon.awssdk.services.emr.model.HadoopJarStepConfig;
import software.amazon.awssdk.services.emr.model.JobFlowInstancesConfig;
import software.amazon.awssdk.services.emr.model.PlacementType;
import software.amazon.awssdk.services.emr.model.RunJobFlowRequest;
import software.amazon.awssdk.services.emr.model.StepConfig;

/**
 * Builds the EMR request (AWS SDK v2 equivalent of the example in the assignment). Pure object
 * construction – no network calls – so it can be unit-tested and printed in dry-run mode.
 *
 * <p>One cluster per run, ONE step that runs the whole driver (the driver chains its MapReduce jobs
 * internally). The cluster terminates by itself when the step ends or fails, to save money.
 */
public final class EmrRequestFactory {

    private EmrRequestFactory() {
    }

    public static RunJobFlowRequest build(EmrSettings s, LaunchRequest r, S3Layout layout) {
        HadoopJarStepConfig jarStep = HadoopJarStepConfig.builder()
                .jar(layout.jarUri())
                .mainClass(s.mainClass(r.implementation()))
                .args(r.stepArgs(layout, s))
                .build();

        StepConfig step = StepConfig.builder()
                .name(r.implementation() + "-" + r.runId())
                .actionOnFailure(ActionOnFailure.TERMINATE_CLUSTER)
                .hadoopJarStep(jarStep)
                .build();

        JobFlowInstancesConfig.Builder instances = JobFlowInstancesConfig.builder()
                .instanceCount(r.instanceCount(s))
                .masterInstanceType(s.instanceType())
                .slaveInstanceType(s.instanceType())
                .keepJobFlowAliveWhenNoSteps(false);   // auto-terminate after the step
        if (s.ec2KeyName() != null) {
            instances.ec2KeyName(s.ec2KeyName());
        }
        if (s.availabilityZone() != null) {
            instances.placement(PlacementType.builder().availabilityZone(s.availabilityZone()).build());
        }

        return RunJobFlowRequest.builder()
                .name("dsp-a2-" + r.implementation() + "-" + r.runId())
                .releaseLabel(s.releaseLabel())
                .applications(Application.builder().name("Hadoop").build())
                .instances(instances.build())
                .steps(step)
                .logUri(layout.logUri())
                .serviceRole(s.serviceRole())
                .jobFlowRole(s.jobFlowRole())
                .build();
    }
}
