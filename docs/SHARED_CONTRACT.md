# החוזה המשותף – Pairs ו-Stripes

מסמך זה מגדיר את כל מה ששני המימושים חייבים לעשות **באופן זהה**, כדי שכל שותף יעבוד לבד ובסוף התוצאות יהיו זהות וההשוואה הוגנת.
הקוד שמממש את החוזה נמצא במודול `common`. **אסור לשכפל או לעקוף אותו** – משתמשים במחלקות שלו.

> שינוי בחוזה או ב-`common` נעשה רק בהסכמת שניכם, ב-commit נפרד ל-`main` (ראו `docs/GIT_WORKFLOW.md`). שאלות פתוחות מרוכזות ב-`docs/OPEN_QUESTIONS.md`.

---

## 1. פורמט הקלט

| | על EMR (הקורפוס האמיתי) | מקומית (בדיקות) |
|---|---|---|
| נתיב | `s3://datasets.elasticmapreduce/ngrams/books/20090715/eng-us-all/3gram/data` | `data/test-input.txt` |
| פורמט קובץ | SequenceFile, דחיסת LZO ברמת בלוק | קובץ טקסט UTF-8 |
| Key | `LongWritable` – מספר שורה | `LongWritable` – byte offset |
| Value | `Text` – שורה אחת | `Text` – שורה אחת |
| דגל | `--input-format sequence` (ברירת מחדל) | `--input-format text` |

פורמט השורה (זהה בשני המקרים), מופרד ב-TAB:

```
ngram TAB year TAB match_count TAB page_count TAB volume_count
coffee black strong	1995	12	11	9
```

- מילים בתוך ה-ngram מופרדות ברווח (רווחים כפולים/מובילים מנורמלים).
- `match_count` = מספר המופעים (instances) של ה-ngram באותה שנה. רק השדה הזה נספר. `page_count` ו-`volume_count` מתעלמים מהם.
- ה-Mapper **מתעלם מה-key** ומשתמש רק ב-value.
- בחירת ה-InputFormat: `JobSetup.configureCorpusInput(job, params)` בלבד.
- לבדיקה מקומית של נתיב ה-SequenceFile: `TextToSequenceFile` (בלי LZO).

## 2. כללי סינון מילים

הסינון מבוצע **רק** דרך `CorpusFilter.forTrigrams(JobSetup.loadStopWords(conf)).apply(line)` ב-Mapper הראשון שקורא את הקורפוס, ותוצאתו מדווחת ב-`CommonCounters.record(context, outcome)`.

סדר הבדיקות (כל שורה נזרקת לפי הכלל הראשון שהיא נכשלת בו):

1. `MALFORMED` – לא ניתנת לפענוח: פחות מ-3 שדות, שנה/ספירה לא מספריות, ספירה ≤ 0, ngram ריק.
2. `WRONG_ARITY` – מספר המילים שונה מ-3.
3. `STOP_WORD` – **לפחות אחת** משלוש המילים היא stop word (בכל מיקום). ההשוואה **לא תלויה ב-case** (`The` = `the`).
4. `KEPT` – השורה נספרת.

כללים נוספים:
- שורה שנזרקה **לא נספרת בשום מקום**, כולל N.
- **אין** נרמול נוסף: לא lowercase של המילים עצמן, לא הסרת פיסוק. `Coffee black strong` ו-`coffee black strong` הם שני trigrams שונים.
- רשימת ה-stop words: `data/stopwords.txt` (נארזת ל-jar כ-resource). אפשר לעקוף עם `--stopwords s3://.../file.txt`, אבל ההרצות המדווחות של שני המימושים חייבות להשתמש **באותו קובץ בדיוק**.
- ⚠️ הרשימה הנוכחית היא placeholder. יש להחליף ברשימה מארכיון המטלה לפני כל הרצת EMR (ראו `OPEN_QUESTIONS.md`, שאלה 4).

## 3. חלוקת הקורפוס לשני חלקים

- מבוצעת ע"י `CorpusSplitter` (וגם מוחזרת ב-`outcome.part()`). אסור לחשב part בדרך אחרת.
- **יחידת החלוקה היא שורה בדאטה** (זוג ngram+שנה, על כל המופעים שלה באותה שנה).
- `part = CRC32(UTF-8("w1 w2 w3" + TAB + year)) mod 2`.
- תכונות: דטרמיניסטית ושחזורית (לא תלויה ב-mapper, ב-split, בהרצה, בפורמט הקלט); trigram שמופיע בכמה שנים מתפזר בין **שני** החלקים; O(1) זיכרון; ~50/50.
- trigram שמופיע בשנה אחת בלבד יהיה בחלק אחד בלבד – זה תקין, ומטופל ע"י r = 0 (סעיף 4).

## 4. נוסחת Deleted Estimation – משמעות המשתנים

```
                     T_r^01 + T_r^10
P_del(w1w2w3) = -------------------------
                 N * (N_r^0 + N_r^1)
```

כל הספירות **אחרי** סינון (סעיף 2):

| משתנה | משמעות |
|---|---|
| `N` | מספר מופעי ה-trigrams בכל הקורפוס (שני החלקים) = סכום `match_count` של השורות שנשמרו = `CommonCounters.N_INSTANCES` |
| `r0`, `r1` | מספר המופעים של ה-trigram בחלק 0 / בחלק 1 |
| `N_r^0` | מספר ה-**types** (trigrams שונים) שמופיעים בדיוק r פעמים בחלק 0 |
| `T_r^01` | סך המופעים **בחלק 1** של ה-types שנספרו ב-`N_r^0` |
| `N_r^1` | מספר ה-types שמופיעים בדיוק r פעמים בחלק 1 |
| `T_r^10` | סך המופעים **בחלק 0** של ה-types שנספרו ב-`N_r^1` |

**הפרשנות המוסכמת לשאלה "איזה r"** (שאלה פתוחה 1): ל-trigram עם `r0` בחלק 0 ו-`r1` בחלק 1:

```
P = (T_{r0}^01 + T_{r1}^10) / (N * (N_{r0}^0 + N_{r1}^1))
```

- "עולם ה-types" = כל ה-trigrams שנראו בקורפוס המסונן. לכן `r = 0` מוגדר היטב: למשל `N_0^0` = מספר ה-types שמופיעים רק בחלק 1, ו-`T_0^01` = סך המופעים שלהם בחלק 1.
- החישוב הסופי **חייב** להיעשות דרך `DeletedEstimation.probability(nr0, nr1, tr01, tr10, n)` (מחשב ב-double כדי למנוע overflow של `N*(N_r^0+N_r^1)`; אותו סדר פעולות → תוצאה זהה ביט-לביט בשני המימושים).
- דוגמת המטלה (bigrams בעברית) נבדקת ב-`ReferenceEstimatorTest`.
- ההסתברות היא של ה-trigram כולו, `P(w1w2w3)`, כפי שהנוסחה מגדירה – ולא הסתברות מותנית (שאלה פתוחה 2).

## 5. פורמט הפלט

- הפלט הסופי נכתב ל-`<outputBase>/final/` (`OutputLayout.finalDir()`), כקובצי `part-r-NNNNN` (+`_SUCCESS`).
- כל שורה (`OutputLine.format`):

```
w1 SPACE w2 SPACE w3 TAB probability
coffee black strong	1.4131897712e-02
```

- ההסתברות בפורמט `%.10e` עם `Locale.ROOT` – רק דרך `OutputLine.formatProbability`. עם `TextOutputFormat`: key=`"w1 w2 w3"`, value=ההסתברות המעוצבת.
- **סדר** (`OutputLine.ORDER`):
  1. `w1` ואז `w2` עולה (סדר בתים של UTF-8 – זהה לסדר של `Text` ב-Hadoop);
  2. הסתברות יורדת;
  3. שובר שוויון (בחירה שלנו): `w3` עולה.
- **סדר גלובלי**: שרשור `part-r-00000, part-r-00001, ...` לפי שם הקובץ חייב לתת רשימה ממוינת. `HashPartitioner` **לא** מספיק – צריך range partitioner (למשל `TotalOrderPartitioner`) או reducer יחיד בשלב המיון (עם הצדקה).
- מחלקת עזר אופציונלית לשלב המיון: `OutputSortKey`.
- שורה לכל trigram שנשמר בסינון (מספר השורות = מספר ה-types).

## 6. מוסכמות שמות

| מה | Pairs | Stripes |
|---|---|---|
| Package | `dsp.a2.pairs` (ותתי-packages) | `dsp.a2.stripes` |
| Driver | `dsp.a2.pairs.PairsDriver` | `dsp.a2.stripes.StripesDriver` |
| מחלקות | `Pairs*` (למשל `PairsCountMapper`) | `Stripes*` |
| Counters ייחודיים | enum ב-package של המימוש | enum ב-package של המימוש |
| מפתחות Configuration ייחודיים | `dsp.a2.pairs.*` | `dsp.a2.stripes.*` |
| שם Job | `pairs-step<K>-<name>` | `stripes-step<K>-<name>` |
| שם שלב (stats, תיקייה) | `step<K>-<name>` (אותיות קטנות, ספרות, מקפים) | אותו דבר |
| jar | `pairs/target/pairs-job.jar` | `stripes/target/stripes-job.jar` |

- תיקיית ביניים של שלב: `OutputLayout.stepDir(K, "<name>")` → `<outputBase>/step<K>-<name>`.
- **Run ID**: `<yyyyMMdd-HHmm>-<la|nola>-<smoke|sample|full>`, למשל `20261201-1830-la-full`. אותיות קטנות, ספרות ומקפים בלבד.
- מפתחות משותפים: `ConfigKeys` (`dsp.a2.*`). אל תשתמשו בקידומת `dsp.a2.` לדברים ייחודיים בלי `pairs`/`stripes`.

## 7. פרמטרים להרצה

שורת פקודה **זהה** לשני ה-Drivers (`RunParameters`):

```
<input> <outputBase> --local-aggregation true|false
        [--input-format sequence|text] [--reducers N] [--stopwords PATH]
```

- `--local-aggregation` **חובה** (כדי שלא תימדד הרצה במצב הלא נכון). המשמעות: כש-`false` – **אין שום** local aggregation באף שלב (לא Combiner ולא in-mapper combining). כש-`true` – כל שלב שיכול ליהנות מ-local aggregation משתמש בה. כל מימוש מתעד באילו שלבים ואיך.
- הערך נקרא ב-Mapper/Driver ע"י `JobSetup.localAggregation(conf)`.
- `--reducers N` – מספר reducers לשלבים שאין להם דרישה משלהם (שלב שמחייב reducer יחיד מתעד זאת).
- אפשרויות Hadoop גנריות (`-D key=value`) נתמכות ע"י `ToolRunner`.
- `JobSetup.apply(conf, params, IMPLEMENTATION)` נקרא **לפני** יצירת Job ראשון. הוא גם מכבה דחיסת map output (סעיף 10).
- N מועבר בין Jobs: `JobSetup.setCorpusN(conf, CommonCounters.value(job1.getCounters(), N_INSTANCES))` → `JobSetup.corpusN(conf)`.
- קוד יציאה של ה-Driver: `0` הצלחה, `1` כשלון Job, `2` שגיאת שימוש.

## 8. נתיבי S3

לכל שותף ולכל הרצה – קידומת נפרדת. **אסור לכתוב תחת הקידומת של השותף השני.**

```
s3://<bucket>/
├── pairs/<runId>/
│   ├── jar/pairs-job.jar        ← ה-jar המדויק שרץ (scripts/upload-jar.sh)
│   ├── output/                  ← outputBase
│   │   ├── step1-.../ ...       ← ביניים (מותר למחוק אחרי ההרצה)
│   │   ├── final/               ← בסיס הידע
│   │   └── _stats/              ← סטטיסטיקות לכל שלב
│   └── logs/                    ← לוגים של EMR
├── stripes/<runId>/ ...         ← אותו מבנה
└── shared/                      ← קבצים משותפים לקריאה בלבד (test-input.txt, stop words מוסכם)
```

- `bucket` מוגדר ב-`config/emr.properties` (לא ב-git). כל שותף יכול להשתמש ב-bucket משלו.
- `outputBase` לא יכול להיות קיים לפני ההרצה – לכן runId חדש לכל הרצה.

## 9. איסוף סטטיסטיקות

- **כל** שלב MapReduce מורץ דרך `StepRunner.run(job, "step<K>-<name>", params.layout().statsDir())`. הוא כותב `<outputBase>/_stats/step<K>-<name>.tsv` ומדפיס שורת סיכום ל-stdout (נשמר גם בלוגים של EMR).
- ההגדרות (זהות לשני המימושים):

| דרישת המטלה | Counter של Hadoop |
|---|---|
| מספר רשומות key-value שנשלחו מה-mappers ל-reducers | `REDUCE_INPUT_RECORDS` (אחרי local aggregation). מדווחים גם `MAP_OUTPUT_RECORDS` (לפני) |
| הגודל שלהן | `MAP_OUTPUT_MATERIALIZED_BYTES` (בתים שנכתבו ל-shuffle אחרי combine, ללא דחיסה). מדווחים גם `MAP_OUTPUT_BYTES` (לפני combine) |
| מספר המפתחות השונים | `REDUCE_INPUT_GROUPS` |

- הערה: Combiner יכול לרוץ גם בצד ה-reduce בזמן merge; לכן `REDUCE_INPUT_RECORDS` הוא "מה שה-reducer קיבל". ההגדרה זהה בשניהם – וזה מה שחשוב להשוואה.
- מדווחים לכל שלב ולכל מצב (`la` / `nola`), וגם סיכום כולל לכל הרצה.
- כלים: `scripts/fetch-results.sh` (מוריד `_stats`), `scripts/stats-table.sh` (טבלת Markdown לדוח).
- counters של הסינון (`CommonCounters`) מופיעים בלוג של שלב 1 ומשמשים להשוואת תקינות (סעיף 11).

## 10. תנאי הרצה זהים להשוואה הוגנת

ההרצות **המדווחות** (4 הרצות מלאות: pairs-la, pairs-nola, stripes-la, stripes-nola) חייבות:

1. אותו קלט מלא (`input.default`) ואותו `--input-format sequence`.
2. אותו `emr.releaseLabel`, `emr.instanceType`, `emr.instanceCount`, אותו region.
3. אותו `data/stopwords.txt` (אותו commit) – ולכן אותו expected output מקומי.
4. אותה גרסת `common` – ה-tag המוסכם (למשל `common-v1`); רושמים את ה-commit של כל jar בדוח.
5. דחיסת map output כבויה (`JobSetup.apply` מבטיח).
6. אותו `--reducers` (או ששניכם משאירים ברירת מחדל).
7. אשכול ייעודי להרצה אחת (launcher יוצר cluster לכל הרצה ומסיים אותו).
8. אין שינוי ב-`-D` מעבר למה שמתועד בדוח.

## 11. השוואת תקינות בין המימושים

1. **מקומית (חובה לפני EMR):** ה-`*AcceptanceTest` של כל מימוש מריץ את ה-Driver על `data/test-input.txt` (text וגם sequence, עם ובלי local aggregation, עם 2 reducers) ומשווה ל-`data/expected/test-input.expected.tsv`, שנוצר ע"י `ReferenceEstimator` (מימוש in-memory של אותה נוסחה, לבדיקות בלבד). אם שניכם עוברים – הפלטים שלכם זהים.
2. **על EMR, בזול:** השוואת `CommonCounters` (`N_INSTANCES`, `KEPT_ROWS`, `KEPT_ROWS_PART0/1`) – חייבים להיות זהים בכל 4 ההרצות. מספר שורות הפלט (`reduce_output_records` של השלב האחרון) – זהה.
3. **10 זוגות הניתוח:** `scripts/top5.sh` על שני הפלטים – אותן 5 השלמות ואותן הסתברויות.
4. **אופציונלי, השוואה מלאה:** `scripts/final-checksum.sh` לשני המימושים – hash זהה ⇔ פלט זהה בתים-לבתים (עולה כסף על העברת נתונים).
5. כלי ההשוואה: `scripts/compare.sh <expected> <actual>` (פורמט, מיון גלובלי, ערכים בסבילות יחסית 1e-9).

## 12. זוגות המילים לניתוח

המטלה דורשת **10** זוגות (שאלה פתוחה 6). כל שותף מנתח 5, וכל 10 שונים. מסכמים את הרשימה **לפני** ההרצות המלאות:

| # | זוג (w1 w2) | אחראי |
|---|---|---|
| 1–5 | _למלא_ | שותף א' (Pairs) → `reports/pairs-analysis.md` |
| 6–10 | _למלא_ | שותף ב' (Stripes) → `reports/stripes-analysis.md` |

שותף ב' מאחד את שני הניתוחים לדוח הסופי.
