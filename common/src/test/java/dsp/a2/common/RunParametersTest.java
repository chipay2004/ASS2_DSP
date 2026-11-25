package dsp.a2.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunParametersTest {

    @Test
    void parsesMinimalArgumentsWithDefaults() {
        RunParameters p = RunParameters.parse(new String[]{"s3://in", "s3://out", "--local-aggregation", "true"});
        assertEquals("s3://in", p.input());
        assertEquals("s3://out", p.outputBase());
        assertTrue(p.localAggregation());
        assertEquals(RunParameters.InputFormat.SEQUENCE, p.inputFormat());
        assertFalse(p.hasReducers());
        assertNull(p.stopWordsPath());
        assertEquals("s3://out/final", p.layout().finalDir());
    }

    @Test
    void parsesAllOptionsInAnyOrder() {
        RunParameters p = RunParameters.parse(new String[]{"in.txt", "out", "--reducers", "4",
                "--input-format", "TEXT", "--stopwords", "s3://b/sw.txt", "--local-aggregation", "False"});
        assertFalse(p.localAggregation());
        assertEquals(RunParameters.InputFormat.TEXT, p.inputFormat());
        assertEquals(4, p.reducers());
        assertEquals("s3://b/sw.txt", p.stopWordsPath());
    }

    @Test
    void localAggregationIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> RunParameters.parse(new String[]{"in", "out"}));
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> RunParameters.parse(new String[]{"in"}));
        assertThrows(IllegalArgumentException.class, () -> RunParameters.parse(null));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"--local-aggregation", "true", "in", "out"}));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"in", "out", "--local-aggregation", "yes"}));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"in", "out", "--local-aggregation"}));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"in", "out", "--local-aggregation", "true", "--bogus", "1"}));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"in", "out", "--local-aggregation", "true", "--reducers", "0"}));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"in", "out", "--local-aggregation", "true", "--reducers", "x"}));
        assertThrows(IllegalArgumentException.class,
                () -> RunParameters.parse(new String[]{"in", "out", "--local-aggregation", "true", "--input-format", "lzo"}));
    }
}
