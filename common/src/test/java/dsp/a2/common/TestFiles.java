package dsp.a2.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Locates files of the repository (data/...) from tests. Maven passes -Drepo.root (see common/pom.xml). */
public final class TestFiles {

    private TestFiles() {
    }

    public static Path repoRoot() {
        return Paths.get(System.getProperty("repo.root", "..")).toAbsolutePath().normalize();
    }

    public static Path testInput() {
        return repoRoot().resolve("data/test-input.txt");
    }

    public static Path expectedOutput() {
        return repoRoot().resolve("data/expected/test-input.expected.tsv");
    }

    public static Path stopWordsFile() {
        return repoRoot().resolve("data/stopwords.txt");
    }
}
