# Stripes – Statistics Report

> Template. Fill every `TODO`. Tables are produced with `scripts/stats-table.sh` from the downloaded
> `_stats/` folders (`scripts/fetch-results.sh stripes <runId>`). Definitions: docs/SHARED_CONTRACT.md §9.

## 1. Run conditions

| | With local aggregation | Without local aggregation |
|---|---|---|
| Run id | TODO (e.g. 20261201-1830-la-full) | TODO (…-nola-full) |
| S3 output | `s3://TODO/stripes/<runId>/output/final` | `s3://TODO/stripes/<runId>/output/final` |
| Git commit of the jar | TODO | TODO |
| common tag | TODO (common-v1) | TODO |
| EMR release / instances | TODO (emr-7.12.0, 3 × m5.xlarge) | TODO |
| `--reducers` | TODO | TODO |
| Stop-word list | `data/stopwords.txt` @ TODO commit | same |
| Total wall time | TODO | TODO |

## 2. Input filtering (step 1 counters – must be identical in all four runs, Pairs and Stripes)

| Counter (`CommonCounters`) | Value |
|---|---|
| INPUT_LINES | TODO |
| MALFORMED | TODO |
| WRONG_ARITY | TODO |
| STOP_WORD_FILTERED | TODO |
| KEPT_ROWS (part 0 / part 1) | TODO (TODO / TODO) |
| **N = N_INSTANCES** (part 0 / part 1) | TODO (TODO / TODO) |
| Output trigrams (final step `reduce_output_records`) | TODO |

## 3. Mapper → reducer traffic per step

Required by the assignment: **records sent from mappers to reducers** (`reduce_input_records`),
**their size** (`map_output_materialized_bytes`, map-output compression off), **number of different keys**
(`reduce_input_groups`).

### 3.1 With local aggregation

TODO: paste `scripts/stats-table.sh results/stripes/<la-run>/_stats`

### 3.2 Without local aggregation

TODO: paste `scripts/stats-table.sh results/stripes/<nola-run>/_stats`

### 3.3 Totals

| | Records sent | Bytes sent | Distinct keys | Time |
|---|---|---|---|---|
| With local aggregation | TODO | TODO | TODO | TODO |
| Without local aggregation | TODO | TODO | TODO | TODO |
| Reduction by local aggregation | TODO % | TODO % | – | TODO % |

## 4. Memory assumptions and their justification

Allowed: the reducer may keep the Hsum table in memory (docs/OPEN_QUESTIONS.md #7). TODO: state exactly
which structures are kept (one merged stripe per `w1 w2`; r → (N_r, T_r)), estimate their maximal size on
the real corpus (largest stripe, number of distinct r) and compare with the container memory. Confirm that
no table r → list of n-grams is ever built.

## 5. Discussion

TODO: Which steps benefit from local aggregation and why; which do not (joins / sort). Skew (e.g. r = 1).
Redundant key-value pairs that were avoided. What dominates time and cost.
