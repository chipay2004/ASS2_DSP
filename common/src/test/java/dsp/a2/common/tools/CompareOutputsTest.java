package dsp.a2.common.tools;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompareOutputsTest {

    private static final List<String> EXPECTED = Arrays.asList(
            "a b c\t5.0000000000e-01",
            "a b d\t2.5000000000e-01",
            "x y z\t1.0000000000e-01");

    @Test
    void identicalOutputsMatch() {
        assertTrue(CompareOutputs.compare(EXPECTED, EXPECTED, 1e-9).ok());
    }

    @Test
    void tinyRelativeDifferenceIsTolerated() {
        List<String> actual = Arrays.asList(
                "a b c\t5.0000000001e-01", "a b d\t2.5000000000e-01", "x y z\t1.0000000000e-01");
        assertTrue(CompareOutputs.compare(EXPECTED, actual, 1e-9).ok());
        assertFalse(CompareOutputs.compare(EXPECTED, actual, 1e-12).ok());
    }

    @Test
    void detectsDifferentProbabilityTrigramAndCount() {
        assertFalse(CompareOutputs.compare(EXPECTED, Arrays.asList(
                "a b c\t5.0000000000e-01", "a b d\t2.6000000000e-01", "x y z\t1.0000000000e-01"), 1e-9).ok());
        assertFalse(CompareOutputs.compare(EXPECTED, Arrays.asList(
                "a b c\t5.0000000000e-01", "a b e\t2.5000000000e-01", "x y z\t1.0000000000e-01"), 1e-9).ok());
        assertFalse(CompareOutputs.compare(EXPECTED, EXPECTED.subList(0, 2), 1e-9).ok());
    }

    @Test
    void detectsUnsortedAndMalformedActual() {
        CompareOutputs.Result unsorted = CompareOutputs.compare(EXPECTED, Arrays.asList(
                "a b d\t2.5000000000e-01", "a b c\t5.0000000000e-01", "x y z\t1.0000000000e-01"), 1e-9);
        assertFalse(unsorted.ok());
        assertTrue(unsorted.problems().get(0).contains("not sorted"));
        assertFalse(CompareOutputs.compare(EXPECTED, Arrays.asList("a b c 0.5"), 1e-9).ok());
    }

    @Test
    void readsHadoopOutputDirectoryInPartOrder() throws Exception {
        Path dir = Files.createTempDirectory("cmp");
        Files.write(dir.resolve("part-r-00001"), Arrays.asList(EXPECTED.get(2)), StandardCharsets.UTF_8);
        Files.write(dir.resolve("part-r-00000"), EXPECTED.subList(0, 2), StandardCharsets.UTF_8);
        Files.write(dir.resolve("_SUCCESS"), new byte[0]);
        assertEquals(EXPECTED, OutputFiles.readLines(dir));
    }
}
