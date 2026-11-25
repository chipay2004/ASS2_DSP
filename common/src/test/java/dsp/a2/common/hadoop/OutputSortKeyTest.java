package dsp.a2.common.hadoop;

import dsp.a2.common.OutputLine;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputSortKeyTest {

    @Test
    void serializationRoundTrip() throws Exception {
        OutputSortKey k = new OutputSortKey("café", "crème", "brûlée", 1.25e-7);
        DataOutputBuffer out = new DataOutputBuffer();
        k.write(out);
        DataInputBuffer in = new DataInputBuffer();
        in.reset(out.getData(), out.getLength());
        OutputSortKey back = new OutputSortKey();
        back.readFields(in);
        assertEquals(k, back);
        assertEquals("café crème brûlée\t1.2500000000e-07", back.toString());
    }

    @Test
    void orderIsTheContractOrder() {
        List<OutputSortKey> keys = new ArrayList<>(Arrays.asList(
                new OutputSortKey("desk", "work", "green", 0.1),
                new OutputSortKey("coffee", "black", "tasty", 0.3),
                new OutputSortKey("coffee", "black", "strong", 0.6),
                new OutputSortKey("coffee", "black", "hot", 0.3)));
        keys.sort(null);
        List<OutputLine> lines = new ArrayList<>();
        for (OutputSortKey k : keys) {
            lines.add(k.toOutputLine());
        }
        List<OutputLine> sorted = new ArrayList<>(lines);
        sorted.sort(OutputLine.ORDER);
        assertEquals(sorted, lines);
        assertEquals("strong", keys.get(0).w3());
        assertEquals("hot", keys.get(1).w3());
    }

    @Test
    void hashDependsOnlyOnThePair() {
        assertEquals(new OutputSortKey("a", "b", "c", 0.1).hashCode(), new OutputSortKey("a", "b", "z", 0.9).hashCode());
        assertTrue(new OutputSortKey("a", "b", "c", 0.1).compareTo(new OutputSortKey("a", "b", "c", 0.1)) == 0);
    }
}
