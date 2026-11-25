package dsp.a2.common.tools;

import dsp.a2.common.OutputLine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints the top-k completions w3 of a word pair "w1 w2" from a final output (file or downloaded
 * directory). Used for the analysis report. Because the output is sorted by (w1 w2, probability desc),
 * these are simply the first k lines of that pair.
 *
 * <pre>
 *   java -cp common/target/common.jar dsp.a2.common.tools.TopCompletions results/pairs/RUN/final "new york" 5
 * </pre>
 */
public final class TopCompletions {

    private TopCompletions() {
    }

    public static List<OutputLine> top(List<String> outputLines, String w1, String w2, int k) {
        List<OutputLine> result = new ArrayList<>();
        for (String line : outputLines) {
            OutputLine l = OutputLine.parse(line);
            if (l.w1().equals(w1) && l.w2().equals(w2)) {
                result.add(l);
            }
        }
        result.sort(OutputLine.ORDER);
        return result.size() > k ? new ArrayList<>(result.subList(0, k)) : result;
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: TopCompletions <output file|dir> \"w1 w2\" [k=5]");
            System.exit(2);
        }
        String[] pair = args[1].trim().split(" +");
        if (pair.length != 2) {
            System.err.println("The pair must be exactly two words: '" + args[1] + "'");
            System.exit(2);
        }
        int k = args.length == 3 ? Integer.parseInt(args[2]) : 5;
        List<OutputLine> top = top(OutputFiles.readLines(Paths.get(args[0])), pair[0], pair[1], k);
        System.out.println("| # | w3 | P(" + pair[0] + " " + pair[1] + " w3) |");
        System.out.println("|---|----|------|");
        for (int i = 0; i < top.size(); i++) {
            System.out.println("| " + (i + 1) + " | " + top.get(i).w3() + " | "
                    + OutputLine.formatProbability(top.get(i).probability()) + " |");
        }
        if (top.isEmpty()) {
            System.out.println("(no trigram starts with '" + args[1] + "')");
        }
    }
}
