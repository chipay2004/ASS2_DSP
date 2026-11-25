package dsp.a2.common.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads a local result: either a single file, or a Hadoop output directory whose {@code part-*} files are
 * concatenated in file-name order (part-r-00000, part-r-00001, ...). Hidden files ({@code _SUCCESS},
 * {@code .crc}) are skipped. Pure Java – no Hadoop needed (download S3 results first, see scripts/).
 */
public final class OutputFiles {

    private OutputFiles() {
    }

    public static List<String> readLines(Path fileOrDir) {
        try {
            if (!Files.isDirectory(fileOrDir)) {
                return Files.readAllLines(fileOrDir, StandardCharsets.UTF_8);
            }
            List<Path> parts;
            try (Stream<Path> s = Files.list(fileOrDir)) {
                parts = s.filter(p -> p.getFileName().toString().startsWith("part-"))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (parts.isEmpty()) {
                throw new IllegalArgumentException("no part-* files in " + fileOrDir);
            }
            List<String> lines = new ArrayList<>();
            for (Path p : parts) {
                lines.addAll(Files.readAllLines(p, StandardCharsets.UTF_8));
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + fileOrDir, e);
        }
    }
}
