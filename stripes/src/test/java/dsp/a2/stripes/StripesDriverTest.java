package dsp.a2.stripes;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Command-line handling of the skeleton driver (independent of the algorithm). */
class StripesDriverTest {

    private static int run(String... args) throws Exception {
        return ToolRunner.run(new Configuration(false), new StripesDriver(), args);
    }

    @Test
    void badArgumentsReturnUsageExitCode() throws Exception {
        assertEquals(2, run());
        assertEquals(2, run("in"));
        assertEquals(2, run("in", "out"));                                   // --local-aggregation missing
        assertEquals(2, run("in", "out", "--local-aggregation", "maybe"));
    }

    @Test
    void implementationNameIsFixed() {
        assertEquals("stripes", StripesDriver.IMPLEMENTATION);
    }
}
