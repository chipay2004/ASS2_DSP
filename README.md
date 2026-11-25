# Assignment 2 – Knowledge-base for Word Prediction (Deleted Estimation on Amazon EMR)

Two MapReduce implementations – **Pairs** and **Stripes** – that compute, for every trigram `w1 w2 w3` of the
Google Books English 3-gram corpus, its Deleted Estimation probability, sorted by `w1 w2` ascending and by
probability descending.

> **Status: shared infrastructure only.** The Pairs and Stripes algorithms are not implemented yet
> (see `docs/PAIRS_TASKS.md`, `docs/STRIPES_TASKS.md`). The final README (names, IDs, run instructions,
> MapReduce steps, S3 output link) is completed at the end – `docs/STRIPES_TASKS.md` §8.

| | Name | Username | ID |
|---|---|---|---|
| Partner A (Pairs) | TODO | TODO | TODO |
| Partner B (Stripes) | TODO | TODO | TODO |

## Repository layout

```text
assignment2/
├── pom.xml                  root POM: modules + ALL versions (Java 11 bytecode, Hadoop 3.4.1, AWS SDK v2, JUnit 5)
├── common/                  shared code, independent of Pairs/Stripes
│   └── src/main/java/dsp/a2/common/
│       ├── NGramRecord, NGramParser, StopWords, CorpusFilter, CorpusSplitter   input handling
│       ├── DeletedEstimation                                                    the formula
│       ├── OutputLine, OutputLayout, RunParameters, ConfigKeys                  shared formats / contract
│       ├── hadoop/   JobSetup, CommonCounters, StepRunner, JobStats, OutputSortKey, LocalHadoop, TextToSequenceFile
│       └── tools/    ReferenceEstimator, CompareOutputs, TopCompletions, StatsTable, OutputFiles
├── pairs/                   Partner A – PairsDriver (skeleton) + acceptance test (disabled until implemented)
├── stripes/                 Partner B – StripesDriver (skeleton) + acceptance test (disabled until implemented)
├── emr-launcher/            submits one EMR run (dry-run by default); depends only on the AWS SDK
├── config/                  emr.properties.example (copy to emr.properties – git-ignored)
├── data/                    stopwords.txt, test-input.txt, expected/test-input.expected.tsv
├── scripts/                 build, local run, compare, upload, launch, fetch, stats/analysis helpers
├── docs/                    SHARED_CONTRACT, PAIRS_TASKS, STRIPES_TASKS, GIT_WORKFLOW, OPEN_QUESTIONS
└── reports/                 statistics and analysis templates for both implementations
```

Module dependencies (no other edges exist):

```text
pairs ──► common ◄── stripes          emr-launcher (stand-alone, AWS SDK only)
```

`pairs` and `stripes` never depend on each other; `ModuleIndependenceTest` in each module fails if that changes.

## Prerequisites

- JDK 17 (any JDK 11+ works; bytecode targets Java 11, EMR 7.x runs Java 17)
- Maven 3.9+
- For EMR only: AWS CLI v2 with credentials configured (`aws configure` / AWS Academy session) – never in the repo
- Optional: a local Hadoop 3.4.x installation (not needed for tests – they run Hadoop in-process)
- Windows: use WSL or Git Bash for `scripts/*.sh`

## Build and test

```bash
mvn clean verify                 # everything
scripts/build.sh pairs           # = mvn -pl pairs -am clean verify     (common + pairs only)
scripts/build.sh stripes         # = mvn -pl stripes -am clean verify   (common + stripes only)
scripts/build.sh launcher        # emr-launcher only
```

Artifacts: `common/target/common.jar`, `pairs/target/pairs-job.jar`, `stripes/target/stripes-job.jar`
(job jars contain common + stop words; Hadoop is `provided`), `emr-launcher/target/emr-launcher.jar`.

Tests:
- `common`: unit tests for parsing, stop words, splitting, the formula (including the assignment's example),
  output format/order, run parameters, statistics, tools, and a real LocalJobRunner job over text and SequenceFile input.
- `pairs` / `stripes`: command-line and module-independence tests now; `*AcceptanceTest` (disabled until the
  algorithm exists) runs the driver on `data/test-input.txt` and compares with the expected output.

## Running locally

```bash
# In-process (no Hadoop installation): remove @Disabled from PairsAcceptanceTest / StripesAcceptanceTest, then
scripts/build.sh pairs

# With a local Hadoop installation:
scripts/run-local.sh pairs true            # output in local-runs/, compared with the expected output

# Expected output for the test input (in-memory reference; small files only):
scripts/make-expected.sh
```

Driver command line (identical for both implementations):

```text
hadoop jar <impl>-job.jar <input> <outputBase> --local-aggregation true|false
           [--input-format sequence|text] [--reducers N] [--stopwords PATH]
```

## Running on Amazon EMR

```bash
cp config/emr.properties.example config/emr.properties     # set s3.bucket (and review instance settings)
scripts/build.sh pairs && scripts/build.sh launcher
RUN=20261201-1830-la-full
scripts/upload-jar.sh pairs $RUN
scripts/launch-emr.sh pairs $RUN true                       # DRY RUN: prints the request, creates nothing
scripts/launch-emr.sh pairs $RUN true --submit              # starts the cluster (costs money)
scripts/fetch-results.sh pairs $RUN                         # downloads _stats/ (add --final for the output)
scripts/stats-table.sh results/pairs/$RUN/_stats            # Markdown table for the report
scripts/top5.sh results/pairs/$RUN/final "new york"         # analysis helper
```

Cheap smoke test first: `scripts/upload-test-input.sh`, then launch with
`--input s3://<bucket>/shared/test-input.txt --input-format text --instance-count 1`.

S3 layout per run: `s3://<bucket>/<impl>/<runId>/{jar,output/{final,_stats,step*},logs}`. The cluster
terminates by itself after its single step. Always check the EMR console afterwards and delete intermediate data.

## Documentation

| File | Content |
|---|---|
| `docs/SHARED_CONTRACT.md` | input/output formats, filtering, corpus split, formula variables, naming, parameters, S3 paths, statistics, fair-comparison rules, correctness checks |
| `docs/PAIRS_TASKS.md` | Partner A: what exists, what to implement, memory rules, order of work, done criteria |
| `docs/STRIPES_TASKS.md` | Partner B: same, plus merging reports, final README and packaging |
| `docs/GIT_WORKFLOW.md` | branches `feature/pairs` / `feature/stripes`, ownership, merging |
| `docs/OPEN_QUESTIONS.md` | ambiguous requirements and the decisions taken |

## Submission (finalized by Partner B)

Zip with all sources (no jars), the output or a link to it on S3, the statistics and analysis reports, and this
README completed with names, IDs, how to run, and notes on the MapReduce steps of both implementations.
