package dsp.a2.common.tools;

import dsp.a2.common.OutputLine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that an output is correct with respect to an expected output:
 * <ol>
 *   <li>every line of ACTUAL follows the contract format;</li>
 *   <li>ACTUAL is sorted by the contract order;</li>
 *   <li>same number of lines, and line i has the same trigram and the same probability
 *       (relative tolerance, default 1e-9) as line i of EXPECTED.</li>
 * </ol>
 * Use it for: reference vs. Pairs, reference vs. Stripes, and Pairs vs. Stripes (full EMR outputs).
 *
 * <pre>
 *   java -cp common/target/common.jar dsp.a2.common.tools.CompareOutputs &lt;expected file|dir&gt; &lt;actual file|dir&gt; [--tolerance 1e-9]
 * </pre>
 * Exit code 0 = equal, 1 = different, 2 = usage error.
 */
public final class CompareOutputs {

    public static final double DEFAULT_TOLERANCE = 1e-9;
    private static final int MAX_REPORTED = 20;

    /** The comparison result: {@code ok()} and a list of human-readable problems. */
    public static final class Result {
        private final List<String> problems = new ArrayList<>();
        private int problemCount;

        void add(String problem) {
            problemCount++;
            if (problems.size() < MAX_REPORTED) {
                problems.add(problem);
            }
        }

        public boolean ok() {
            return problemCount == 0;
        }

        public int problemCount() {
            return problemCount;
        }

        public List<String> problems() {
            return problems;
        }
    }

    private CompareOutputs() {
    }

    public static Result compare(List<String> expected, List<String> actual, double tolerance) {
        Result result = new Result();
        List<OutputLine> exp = parseAll("expected", expected, result);
        List<OutputLine> act = parseAll("actual", actual, result);
        if (!result.ok()) {
            return result;
        }
        for (int i = 1; i < act.size(); i++) {
            if (OutputLine.ORDER.compare(act.get(i - 1), act.get(i)) > 0) {
                result.add("actual is not sorted at line " + (i + 1) + ": '" + act.get(i - 1) + "' before '"
                        + act.get(i) + "'");
            }
        }
        if (exp.size() != act.size()) {
            result.add("line count differs: expected " + exp.size() + ", actual " + act.size());
        }
        int n = Math.min(exp.size(), act.size());
        for (int i = 0; i < n; i++) {
            OutputLine e = exp.get(i);
            OutputLine a = act.get(i);
            if (!e.trigram().equals(a.trigram())) {
                result.add("line " + (i + 1) + ": trigram differs: expected '" + e.trigram() + "', actual '"
                        + a.trigram() + "'");
            } else if (!close(e.probability(), a.probability(), tolerance)) {
                result.add("line " + (i + 1) + " (" + e.trigram() + "): probability differs: expected "
                        + e.probability() + ", actual " + a.probability());
            }
        }
        return result;
    }

    static boolean close(double a, double b, double relTol) {
        if (a == b) {
            return true;
        }
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return Math.abs(a - b) <= relTol * scale;
    }

    private static List<OutputLine> parseAll(String name, List<String> lines, Result result) {
        List<OutputLine> parsed = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            try {
                parsed.add(OutputLine.parse(lines.get(i)));
            } catch (IllegalArgumentException e) {
                result.add(name + " line " + (i + 1) + " is not in the output format: " + e.getMessage());
            }
        }
        return parsed;
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 4) {
            System.err.println("Usage: CompareOutputs <expected file|dir> <actual file|dir> [--tolerance X]");
            System.exit(2);
        }
        double tol = DEFAULT_TOLERANCE;
        if (args.length == 4) {
            if (!"--tolerance".equals(args[2])) {
                System.err.println("unknown option " + args[2]);
                System.exit(2);
            }
            tol = Double.parseDouble(args[3]);
        }
        List<String> expected = OutputFiles.readLines(Paths.get(args[0]));
        List<String> actual = OutputFiles.readLines(Paths.get(args[1]));
        Result r = compare(expected, actual, tol);
        if (r.ok()) {
            System.out.println("OK: " + actual.size() + " lines match (tolerance " + tol + ")");
            System.exit(0);
        }
        System.out.println("DIFFERENT: " + r.problemCount() + " problem(s). First ones:");
        for (String p : r.problems()) {
            System.out.println("  - " + p);
        }
        System.exit(1);
    }
}
