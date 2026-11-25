package dsp.a2.common.hadoop;

import dsp.a2.common.tools.StatsTable;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.TaskCounter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobStatsTest {

    private static Counters sampleCounters() {
        Counters c = new Counters();
        c.findCounter(TaskCounter.MAP_OUTPUT_RECORDS).setValue(300);
        c.findCounter(TaskCounter.MAP_OUTPUT_BYTES).setValue(9000);
        c.findCounter(TaskCounter.MAP_OUTPUT_MATERIALIZED_BYTES).setValue(4000);
        c.findCounter(TaskCounter.COMBINE_OUTPUT_RECORDS).setValue(120);
        c.findCounter(TaskCounter.REDUCE_INPUT_RECORDS).setValue(120);
        c.findCounter(TaskCounter.REDUCE_INPUT_GROUPS).setValue(80);
        return c;
    }

    @Test
    void collectsTheRequiredCounters() {
        JobStats s = JobStats.from("pairs", "step1-count", true, true, 4, 12500, sampleCounters());
        assertEquals(120, s.get(TaskCounter.REDUCE_INPUT_RECORDS));
        assertEquals(4000, s.get(TaskCounter.MAP_OUTPUT_MATERIALIZED_BYTES));
        assertEquals(80, s.get(TaskCounter.REDUCE_INPUT_GROUPS));
        assertEquals(0, s.get(TaskCounter.MAP_INPUT_RECORDS));
        assertTrue(s.toHumanReadable().contains("sent-to-reducers=120 records"));
    }

    @Test
    void tsvRowMatchesHeader() {
        JobStats s = JobStats.from("stripes", "step2-x", false, true, 1, 1000, sampleCounters());
        assertEquals(JobStats.tsvHeader().split("\t").length, s.toTsvRow().split("\t").length);
        assertTrue(s.toTsvRow().startsWith("stripes\tstep2-x\tfalse\ttrue\t1\t1000\t"));
    }

    @Test
    void nullCountersGiveZeros() {
        JobStats s = JobStats.from("pairs", "step1", true, false, 1, 0, null);
        assertEquals(0, s.get(TaskCounter.REDUCE_INPUT_RECORDS));
    }

    @Test
    void writesTsvFileReadableByStatsTable() throws Exception {
        Path dir = Files.createTempDirectory("stats");
        JobStats s = JobStats.from("pairs", "step1-count", true, true, 4, 12500, sampleCounters());
        s.writeTo(LocalHadoop.conf(), dir.toString());
        String content = new String(Files.readAllBytes(dir.resolve("step1-count.tsv")), StandardCharsets.UTF_8);
        String md = StatsTable.toMarkdown(Collections.singletonList(content));
        assertTrue(md.contains("| pairs | step1-count | true | true | 12.5 | 300 | 9000 | 120 | 120 | 4000 | 80 |"), md);
    }
}
