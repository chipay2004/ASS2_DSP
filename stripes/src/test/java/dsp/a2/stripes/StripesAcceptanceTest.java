package dsp.a2.stripes;

import dsp.a2.common.OutputLayout;
import dsp.a2.common.hadoop.LocalHadoop;
import dsp.a2.common.hadoop.TextToSequenceFile;
import dsp.a2.common.tools.CompareOutputs;
import dsp.a2.common.tools.OutputFiles;
import org.apache.hadoop.util.ToolRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ACCEPTANCE TEST of the Stripes implementation – already written, so "done" is well defined.
 *
 * <p>Runs the real driver in-process (Hadoop LocalJobRunner, no installation needed) on
 * data/test-input.txt and checks the final output against data/expected/test-input.expected.tsv, which
 * was produced by the shared in-memory reference estimator. It also checks that statistics were written.
 *
 * <p>Remove the {@code @Disabled} annotation once StripesDriver is implemented (docs/STRIPES_TASKS.md).
 */
@Disabled("Enable when StripesDriver is implemented - see docs/STRIPES_TASKS.md")
class StripesAcceptanceTest {

    private static Path repoRoot() {
        return Paths.get(System.getProperty("repo.root", "..")).toAbsolutePath().normalize();
    }

    @Test
    void textInputWithLocalAggregation() throws Exception {
        runAndCompare(repoRoot().resolve("data/test-input.txt").toString(), "text", true);
    }

    @Test
    void textInputWithoutLocalAggregation() throws Exception {
        runAndCompare(repoRoot().resolve("data/test-input.txt").toString(), "text", false);
    }

    @Test
    void sequenceFileInputLikeOnEmr() throws Exception {
        Path seq = Files.createTempDirectory("stripes-seq").resolve("input.seq");
        TextToSequenceFile.convert(LocalHadoop.conf(), repoRoot().resolve("data/test-input.txt").toString(),
                new org.apache.hadoop.fs.Path(seq.toString()));
        runAndCompare(seq.toString(), "sequence", true);
    }

    private static void runAndCompare(String input, String format, boolean localAggregation) throws Exception {
        Path outputBase = Files.createTempDirectory("stripes-it").resolve("run");
        int rc = ToolRunner.run(LocalHadoop.conf(), new StripesDriver(), new String[]{
                input, outputBase.toString(),
                "--local-aggregation", String.valueOf(localAggregation),
                "--input-format", format,
                "--reducers", "2"});  // more than one reducer: global order must still hold
        assertEquals(0, rc, "driver exit code");

        OutputLayout layout = new OutputLayout(outputBase.toString());
        List<String> expected = OutputFiles.readLines(repoRoot().resolve("data/expected/test-input.expected.tsv"));
        List<String> actual = OutputFiles.readLines(Paths.get(layout.finalDir()));
        CompareOutputs.Result result = CompareOutputs.compare(expected, actual, CompareOutputs.DEFAULT_TOLERANCE);
        assertTrue(result.ok(), String.join("\n", result.problems()));

        try (Stream<Path> stats = Files.list(Paths.get(layout.statsDir()))) {
            assertTrue(stats.anyMatch(p -> p.toString().endsWith(".tsv")), "no statistics written to _stats/");
        }
    }
}
