package dsp.a2.common.hadoop;

import dsp.a2.common.CorpusFilter;
import dsp.a2.common.RunParameters;
import dsp.a2.common.StopWords;
import dsp.a2.common.TestFiles;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs a real (LocalJobRunner) map-only job over data/test-input.txt, once as text and once converted to a
 * SequenceFile, and checks that the shared input wiring + filter + counters behave identically.
 * This exercises exactly what the FIRST mapper of Pairs and of Stripes will do with the corpus.
 */
class LocalPipelineTest {

    /** Test-only mapper: apply the shared filter, update the shared counters, emit nothing. */
    public static class FilterOnlyMapper extends Mapper<LongWritable, Text, NullWritable, NullWritable> {
        private CorpusFilter filter;

        @Override
        protected void setup(Context context) throws IOException {
            filter = CorpusFilter.forTrigrams(JobSetup.loadStopWords(context.getConfiguration()));
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) {
            CommonCounters.record(context, filter.apply(value.toString()));
        }
    }

    private static Counters run(RunParameters p) throws Exception {
        Configuration conf = LocalHadoop.conf();
        JobSetup.apply(conf, p, "common-test");
        Job job = Job.getInstance(conf, "common-local-pipeline-test");
        job.setJarByClass(FilterOnlyMapper.class);
        job.setMapperClass(FilterOnlyMapper.class);
        job.setNumReduceTasks(0);
        job.setOutputFormatClass(NullOutputFormat.class);
        JobSetup.configureCorpusInput(job, p);
        FileOutputFormat.setOutputPath(job, new Path(p.layout().stepDir(1, "filter")));
        assertTrue(StepRunner.run(job, "step1-filter", p.layout().statsDir()));
        assertTrue(Files.exists(java.nio.file.Paths.get(p.layout().statsDir(), "step1-filter.tsv")));
        return job.getCounters();
    }

    @Test
    void textAndSequenceInputGiveTheSameCountsAsThePureJavaFilter() throws Exception {
        // Expected values computed with the pure-Java filter.
        List<String> lines = Files.readAllLines(TestFiles.testInput(), StandardCharsets.UTF_8);
        CorpusFilter filter = CorpusFilter.forTrigrams(StopWords.loadDefault());
        long kept = 0;
        long n = 0;
        long stop = 0;
        long n0 = 0;
        for (String l : lines) {
            CorpusFilter.Outcome o = filter.apply(l);
            if (o.isKept()) {
                kept++;
                n += o.record().matchCount();
                if (o.part() == 0) {
                    n0 += o.record().matchCount();
                }
            } else if (o.status() == CorpusFilter.Status.STOP_WORD) {
                stop++;
            }
        }

        java.nio.file.Path tmp = Files.createTempDirectory("pipeline");
        Counters text = run(RunParameters.parse(new String[]{TestFiles.testInput().toString(),
                tmp.resolve("text-run").toString(), "--local-aggregation", "false", "--input-format", "text"}));

        Path seqFile = new Path(tmp.resolve("input.seq").toString());
        assertEquals(lines.size(), TextToSequenceFile.convert(LocalHadoop.conf(), TestFiles.testInput().toString(), seqFile));
        Counters seq = run(RunParameters.parse(new String[]{seqFile.toString(),
                tmp.resolve("seq-run").toString(), "--local-aggregation", "false", "--input-format", "sequence"}));

        for (Counters c : new Counters[]{text, seq}) {
            assertEquals(lines.size(), CommonCounters.value(c, CommonCounters.INPUT_LINES));
            assertEquals(kept, CommonCounters.value(c, CommonCounters.KEPT_ROWS));
            assertEquals(stop, CommonCounters.value(c, CommonCounters.STOP_WORD_FILTERED));
            assertEquals(n, CommonCounters.value(c, CommonCounters.N_INSTANCES));
            assertEquals(n0, CommonCounters.value(c, CommonCounters.N_INSTANCES_PART0));
            assertEquals(n, CommonCounters.value(c, CommonCounters.N_INSTANCES_PART0)
                    + CommonCounters.value(c, CommonCounters.N_INSTANCES_PART1));
            assertEquals(kept, CommonCounters.value(c, CommonCounters.KEPT_ROWS_PART0)
                    + CommonCounters.value(c, CommonCounters.KEPT_ROWS_PART1));
            assertEquals(5, CommonCounters.value(c, CommonCounters.MALFORMED));
            assertEquals(2, CommonCounters.value(c, CommonCounters.WRONG_ARITY));
        }
    }
}
