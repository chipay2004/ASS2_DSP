package dsp.a2.common.tools;

import dsp.a2.common.OutputLine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportToolsTest {

    @Test
    void topCompletionsReturnsBestKOfThePair() {
        List<String> lines = Arrays.asList(
                "new york city\t5.0000000000e-01",
                "new york times\t3.0000000000e-01",
                "new york state\t1.0000000000e-01",
                "new zealand team\t9.0000000000e-01");
        List<OutputLine> top = TopCompletions.top(lines, "new", "york", 2);
        assertEquals(2, top.size());
        assertEquals("city", top.get(0).w3());
        assertEquals("times", top.get(1).w3());
        assertTrue(TopCompletions.top(lines, "old", "york", 5).isEmpty());
    }

    @Test
    void statsTableRendersMarkdownRow() {
        String tsv = "implementation\tstep\tlocal_aggregation\tsucceeded\treduce_tasks\telapsed_ms\t"
                + "map_input_records\tmap_output_records\tmap_output_bytes\tmap_output_materialized_bytes\t"
                + "combine_input_records\tcombine_output_records\treduce_input_groups\treduce_input_records\t"
                + "reduce_shuffle_bytes\treduce_output_records\tspilled_records\n"
                + "pairs\tstep1-count\ttrue\ttrue\t4\t12500\t100\t300\t9000\t4000\t300\t120\t80\t120\t4000\t80\t240\n";
        String md = StatsTable.toMarkdown(Collections.singletonList(tsv));
        String[] lines = md.split("\n");
        assertEquals(3, lines.length);
        assertEquals("| pairs | step1-count | true | true | 12.5 | 300 | 9000 | 120 | 120 | 4000 | 80 |", lines[2]);
    }
}
