package dsp.a2.common.hadoop;

import dsp.a2.common.ConfigKeys;
import dsp.a2.common.RunParameters;
import dsp.a2.common.StopWords;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSetupTest {

    private static RunParameters params(String... extra) {
        String[] base = {"in1,in2", "out", "--local-aggregation", "true"};
        String[] all = Arrays.copyOf(base, base.length + extra.length);
        System.arraycopy(extra, 0, all, base.length, extra.length);
        return RunParameters.parse(all);
    }

    @Test
    void applyCopiesParametersAndDisablesMapOutputCompression() {
        Configuration conf = LocalHadoop.conf();
        conf.setBoolean(JobSetup.MAP_OUTPUT_COMPRESS, true);
        JobSetup.apply(conf, params("--input-format", "text", "--stopwords", "/tmp/sw.txt"), "pairs");
        assertEquals("pairs", conf.get(ConfigKeys.IMPLEMENTATION));
        assertTrue(JobSetup.localAggregation(conf));
        assertEquals("text", conf.get(ConfigKeys.INPUT_FORMAT));
        assertEquals("/tmp/sw.txt", conf.get(ConfigKeys.STOPWORDS_PATH));
        assertFalse(conf.getBoolean(JobSetup.MAP_OUTPUT_COMPRESS, true));
    }

    @Test
    void selectsInputFormatAndPaths() throws Exception {
        Job seq = Job.getInstance(LocalHadoop.conf());
        JobSetup.configureCorpusInput(seq, params());
        assertEquals(SequenceFileInputFormat.class, seq.getInputFormatClass());

        Job text = Job.getInstance(LocalHadoop.conf());
        JobSetup.configureCorpusInput(text, params("--input-format", "text"));
        assertEquals(TextInputFormat.class, text.getInputFormatClass());
        assertTrue(text.getConfiguration().get("mapreduce.input.fileinputformat.inputdir").contains("in2"));
    }

    @Test
    void reducersOnlyWhenGiven() throws Exception {
        Job job = Job.getInstance(LocalHadoop.conf());
        job.setNumReduceTasks(7);
        JobSetup.configureReducers(job, params());
        assertEquals(7, job.getNumReduceTasks());
        JobSetup.configureReducers(job, params("--reducers", "3"));
        assertEquals(3, job.getNumReduceTasks());
    }

    @Test
    void corpusNRoundTripAndValidation() {
        Configuration conf = new Configuration(false);
        assertThrows(IllegalStateException.class, () -> JobSetup.corpusN(conf));
        assertThrows(IllegalArgumentException.class, () -> JobSetup.setCorpusN(conf, 0));
        JobSetup.setCorpusN(conf, 12_345_678_901L);
        assertEquals(12_345_678_901L, JobSetup.corpusN(conf));
    }

    @Test
    void loadsPackagedOrCustomStopWords() throws Exception {
        Configuration conf = LocalHadoop.conf();
        assertTrue(JobSetup.loadStopWords(conf).isStopWord("the"));

        Path file = Files.createTempFile("sw", ".txt");
        Files.write(file, Arrays.asList("# custom", "coffee"), StandardCharsets.UTF_8);
        conf.set(ConfigKeys.STOPWORDS_PATH, file.toUri().toString());
        StopWords custom = JobSetup.loadStopWords(conf);
        assertEquals(1, custom.size());
        assertTrue(custom.isStopWord("Coffee"));
        assertFalse(custom.isStopWord("the"));
    }
}
