package dsp.a2.common.tools;

import dsp.a2.common.CorpusFilter;
import dsp.a2.common.DeletedEstimation;
import dsp.a2.common.OutputLine;
import dsp.a2.common.StopWords;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * IN-MEMORY reference ("oracle") implementation of Deleted Estimation, for SMALL local files only.
 *
 * <p>It is NOT a MapReduce implementation and must never be used inside a job: it keeps every trigram
 * and the whole r → statistics table in memory, which the assignment explicitly forbids for the real
 * corpus. Its only purpose is to produce the EXPECTED output for the local test files, so that each
 * partner can verify his MapReduce implementation independently:
 *
 * <pre>
 *   java -cp common/target/common.jar dsp.a2.common.tools.ReferenceEstimator data/test-input.txt out.tsv
 *   java -cp common/target/common.jar dsp.a2.common.tools.CompareOutputs out.tsv local-runs/pairs-.../final
 * </pre>
 * It uses exactly the shared building blocks (CorpusFilter, CorpusSplitter, DeletedEstimation, OutputLine).
 */
public final class ReferenceEstimator {

    private ReferenceEstimator() {
    }

    /** The per-part statistics of the formula for one value of r. */
    static final class RStats {
        long nr0;   // N_r^0
        long tr01;  // T_r^01
        long nr1;   // N_r^1
        long tr10;  // T_r^10
    }

    /**
     * Core computation on already-split counts, independent of n (works for the bigram example in the
     * assignment too).
     *
     * @param counts type → {count in part 0, count in part 1}; every type must have a positive total
     * @param n      N, the number of instances in the whole corpus
     * @return type → probability
     */
    public static Map<String, Double> estimate(Map<String, long[]> counts, long n) {
        Map<Long, RStats> table = buildTable(counts);
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, long[]> e : counts.entrySet()) {
            RStats a = table.get(e.getValue()[0]);
            RStats b = table.get(e.getValue()[1]);
            result.put(e.getKey(), DeletedEstimation.probability(a.nr0, b.nr1, a.tr01, b.tr10, n));
        }
        return result;
    }

    /** r → (N_r^0, T_r^01, N_r^1, T_r^10), including r = 0. Package-private for tests. */
    static Map<Long, RStats> buildTable(Map<String, long[]> counts) {
        Map<Long, RStats> table = new HashMap<>();
        for (long[] c : counts.values()) {
            long r0 = c[0];
            long r1 = c[1];
            if (r0 < 0 || r1 < 0 || r0 + r1 == 0) {
                throw new IllegalArgumentException("invalid counts " + r0 + "," + r1);
            }
            RStats s0 = table.computeIfAbsent(r0, k -> new RStats());
            s0.nr0 += 1;
            s0.tr01 += r1;
            RStats s1 = table.computeIfAbsent(r1, k -> new RStats());
            s1.nr1 += 1;
            s1.tr10 += r0;
        }
        return table;
    }

    /** Full pipeline on raw dataset lines: filter → split → count → estimate → sort. */
    public static List<OutputLine> fromLines(Iterable<String> lines, StopWords stopWords) {
        CorpusFilter filter = CorpusFilter.forTrigrams(stopWords);
        Map<String, long[]> counts = new TreeMap<>();
        long n = 0;
        for (String line : lines) {
            CorpusFilter.Outcome o = filter.apply(line);
            if (!o.isKept()) {
                continue;
            }
            long c = o.record().matchCount();
            counts.computeIfAbsent(o.record().ngramText(), k -> new long[2])[o.part()] += c;
            n += c;
        }
        List<OutputLine> out = new ArrayList<>();
        if (counts.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, Double> e : estimate(counts, n).entrySet()) {
            String[] w = e.getKey().split(" ");
            out.add(new OutputLine(w[0], w[1], w[2], e.getValue()));
        }
        out.sort(OutputLine.ORDER);
        return out;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 4) {
            System.err.println("Usage: ReferenceEstimator <input.txt> <output.tsv> [--stopwords <file>]");
            System.exit(2);
        }
        StopWords stopWords;
        if (args.length == 4) {
            if (!"--stopwords".equals(args[2])) {
                System.err.println("unknown option " + args[2]);
                System.exit(2);
            }
            try (Reader r = Files.newBufferedReader(Paths.get(args[3]), StandardCharsets.UTF_8)) {
                stopWords = StopWords.fromReader(r);
            }
        } else {
            stopWords = StopWords.loadDefault();
        }
        List<String> lines = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        List<OutputLine> result = fromLines(lines, stopWords);
        Path out = Paths.get(args[1]);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (OutputLine l : result) {
                w.write(l.format());
                w.write('\n');
            }
        }
        System.out.println("Wrote " + result.size() + " trigrams to " + out);
    }
}
