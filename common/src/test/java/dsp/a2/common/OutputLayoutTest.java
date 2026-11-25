package dsp.a2.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutputLayoutTest {

    @Test
    void buildsPathsUnderBase() {
        OutputLayout l = new OutputLayout("s3://bucket/pairs/run-01/output/");
        assertEquals("s3://bucket/pairs/run-01/output", l.base());
        assertEquals("s3://bucket/pairs/run-01/output/step1-count", l.stepDir(1, "count"));
        assertEquals("s3://bucket/pairs/run-01/output/final", l.finalDir());
        assertEquals("s3://bucket/pairs/run-01/output/_stats", l.statsDir());
    }

    @Test
    void worksWithLocalPaths() {
        assertEquals("local-runs/x/step2-join-r", new OutputLayout("local-runs/x").stepDir(2, "join-r"));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> new OutputLayout(" "));
        OutputLayout l = new OutputLayout("out");
        assertThrows(IllegalArgumentException.class, () -> l.stepDir(0, "count"));
        assertThrows(IllegalArgumentException.class, () -> l.stepDir(1, "Bad Name"));
    }
}
