package dsp.a2.common.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Turns the per-step statistics TSV files ({@code <outputBase>/_stats/*.tsv}, downloaded locally) into a
 * Markdown table ready to paste into reports/*-statistics.md. Several directories can be given (e.g. the
 * run with and the run without local aggregation) and become one table.
 *
 * <pre>
 *   java -cp common/target/common.jar dsp.a2.common.tools.StatsTable results/pairs/RUN-la/_stats results/pairs/RUN-nola/_stats
 * </pre>
 */
public final class StatsTable {

    /** Columns shown in the report (subset of JobStats.COLUMNS), in this order. */
    static final String[] SHOWN = {
            "implementation", "step", "local_aggregation", "succeeded", "elapsed_ms",
            "map_output_records", "map_output_bytes", "combine_output_records",
            "reduce_input_records", "map_output_materialized_bytes", "reduce_input_groups"
    };

    static final String[] TITLES = {
            "Impl", "Step", "Local agg.", "OK", "Time (s)",
            "Map output records (before agg.)", "Map output bytes (before agg.)", "Combine output records",
            "**Records sent to reducers**", "**Size sent (bytes)**", "**Distinct keys**"
    };

    private StatsTable() {
    }

    public static String toMarkdown(List<String> tsvContents) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", TITLES)).append(" |\n");
        sb.append("|");
        for (int i = 0; i < TITLES.length; i++) {
            sb.append("---|");
        }
        sb.append('\n');
        for (String content : tsvContents) {
            String[] lines = content.split("\n");
            if (lines.length < 2) {
                continue;
            }
            String[] header = lines[0].trim().split("\t");
            for (int li = 1; li < lines.length; li++) {
                if (lines[li].trim().isEmpty()) {
                    continue;
                }
                String[] row = lines[li].split("\t", -1);
                sb.append("|");
                for (String col : SHOWN) {
                    String v = valueOf(header, row, col);
                    if (col.equals("elapsed_ms") && !v.isEmpty()) {
                        v = String.format(java.util.Locale.ROOT, "%.1f", Long.parseLong(v) / 1000.0);
                    }
                    sb.append(' ').append(v).append(" |");
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String valueOf(String[] header, String[] row, String column) {
        for (int i = 0; i < header.length && i < row.length; i++) {
            if (header[i].equals(column)) {
                return row[i];
            }
        }
        return "";
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: StatsTable <stats dir or .tsv file>...");
            System.exit(2);
        }
        List<String> contents = new ArrayList<>();
        for (String a : args) {
            Path p = Paths.get(a);
            List<Path> files;
            if (Files.isDirectory(p)) {
                try (Stream<Path> s = Files.list(p)) {
                    files = s.filter(f -> f.toString().endsWith(".tsv")).sorted().collect(Collectors.toList());
                }
            } else {
                files = List.of(p);
            }
            for (Path f : files) {
                contents.add(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
            }
        }
        System.out.print(toMarkdown(contents));
    }
}
