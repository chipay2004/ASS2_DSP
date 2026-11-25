# משימות שותף א' – מימוש Pairs

ענף: `feature/pairs` · מודול: `pairs/` · Driver: `dsp.a2.pairs.PairsDriver`
קראו קודם: `docs/SHARED_CONTRACT.md` ו-`docs/OPEN_QUESTIONS.md`.

## 1. תחום האחריות

| מותר לשנות | לא משנים (משותף – רק דרך `main` בהסכמה) |
|---|---|
| `pairs/**` | `common/**`, `pom.xml` (שורש), `data/**` |
| `reports/pairs-statistics.md`, `reports/pairs-analysis.md` | `emr-launcher/**`, `scripts/**`, `config/*.example` |
| סימון התקדמות במסמך הזה | `docs/SHARED_CONTRACT.md`, `stripes/**` |

עזר שאתה צריך ורק Pairs משתמש בו → בתוך `pairs/`, לא ב-`common`.

## 2. מה כבר קיים – ובמה להשתמש

| מחלקה (`common`) | למה |
|---|---|
| `RunParameters` | פענוח שורת הפקודה (כבר מחובר ב-`PairsDriver`) |
| `JobSetup` | `apply` (כבר נקרא), `configureCorpusInput`, `configureReducers`, `loadStopWords`, `localAggregation`, `setCorpusN` / `corpusN` |
| `CorpusFilter` + `CommonCounters.record` | **חובה** ב-Mapper הראשון: פענוח, סינון, part, counters |
| `NGramRecord`, `NGramParser`, `StopWords`, `CorpusSplitter` | בשימוש דרך `CorpusFilter` |
| `DeletedEstimation.probability` | **חובה** לחישוב ההסתברות |
| `OutputLine` / `OutputSortKey` | פורמט וסדר הפלט הסופי |
| `OutputLayout` | נתיבי `step<K>-<name>`, `final`, `_stats` |
| `StepRunner.run` | **חובה** להרצת כל שלב (סטטיסטיקות) |
| `LocalHadoop`, `TextToSequenceFile` | בדיקות מקומיות |
| `ReferenceEstimator`, `CompareOutputs`, `TopCompletions`, `StatsTable` | expected, השוואה, ניתוח, טבלאות |

קבצי בדיקה מוכנים: `pairs/src/test/java/dsp/a2/pairs/PairsAcceptanceTest.java` (כרגע `@Disabled`), `PairsDriverTest`, `ModuleIndependenceTest`.

## 3. מה עליך לממש

1. כל שלבי ה-MapReduce של Pairs (Mappers, Reducers, Combiners, Partitioners, Comparators, Writables) תחת `dsp.a2.pairs`.
2. שרשור השלבים ב-`PairsDriver.run` (מחליף את ה-`UnsupportedOperationException`).
3. מצב עם ובלי local aggregation לפי `JobSetup.localAggregation(conf)`.
4. שלב מיון שמייצר פלט ממוין **גלובלית** ב-`finalDir()`.
5. תיעוד המימוש והנחות הזיכרון (סעיף 5) – ב-Javadoc של המחלקות ובדוח.

## 4. הנחות הזיכרון של Pairs (המחמירות)

- **אסור להניח שום דבר על גודל הזיכרון.** כל Mapper/Reducer מחזיק O(1) זיכרון ביחס לגודל הקלט: אין `HashMap` של trigrams, אין רשימת ערכים של key בזיכרון, אין טבלת r → (N_r, T_r) בזיכרון.
- מותר: רשימת ה-stop words (קבועה, כמה מאות מילים), N (מספר אחד), משתנים בודדים.
- טבלת r → רשימת n-grams – אסורה בכל מימוש.
- local aggregation: עדיף **Combiner**. in-mapper combining מותר רק עם חוצץ בגודל קבוע שמתרוקן (flush) כשהוא מתמלא – ואז צריך להצדיק שזה O(1).
- כל join בין trigram לסטטיסטיקות של ה-r שלו נעשה דרך MapReduce (secondary sort / order inversion), לא בזיכרון.
- להימנע מזוגות key-value מיותרים (דרישה מפורשת): לא לפלוט רשומות ש"רק עוברות הלאה", N עובר כ-counter ולא כרשומות.

## 5. תכנון מוצע לשלבים (הצעה – אתה מחליט ומצדיק)

| שלב | Map | Reduce | Local aggregation |
|---|---|---|---|
| `step1-count` | `CorpusFilter` → key=`w1 w2 w3`, value=(c0, c1) לפי `part` | סכום → `trigram r0 r1` | Combiner סוכם (c0,c1) |
| `step2-rstats` | מכל trigram: key=(0, r0) value=(1, r1); key=(1, r1) value=(1, r0) | סכום → `part r N_r T_r` | Combiner סוכם |
| `step3-join` | שני קלטים (step1, step2) → key=(part, r, tag) כש-tag של הסטטיסטיקה קטן יותר | partition לפי (part, r), סטטיסטיקה מגיעה ראשונה → זורם על ה-trigrams ופולט (trigram, part, N, T) | – |
| `step4-prob` | key=trigram | שני ערכים (part 0, part 1) → `DeletedEstimation.probability` עם `JobSetup.corpusN` | – |
| `step5-sort` | key=`OutputSortKey` | identity → `finalDir()` | – (range partitioner / reducer יחיד) |

נקודות שחשוב להבין ולהסביר בהגנה:
- N נלקח מ-`CommonCounters.N_INSTANCES` של step1 ועובר ב-Configuration.
- ב-step3 ה-key `(part, r=1)` מקבל את רוב ה-trigrams (skew). זה עדיין O(1) זיכרון אבל איטי; אפשר לשקול salting (שכפול רשומת הסטטיסטיקה ל-k partitions).
- trigram עם r0=0 או r1=0 – מקבל את הסטטיסטיקה של r=0 (מוגדרת, ראו החוזה §4).
- למה `HashPartitioner` לא נותן סדר גלובלי, ואיך פותרים.

## 6. סדר עבודה וקריטריוני סיום

- [ ] **0. הכנה** – `git switch feature/pairs`; `scripts/build.sh pairs` עובר (3 בדיקות עוברות, 3 מדולגות).
  ✔ כשה-build ירוק אצלך מקומית.
- [ ] **1. step1-count** – Mapper עם `CorpusFilter` + `CommonCounters.record`, Reducer, Combiner.
  ✔ בדיקת יחידה ב-`pairs/src/test` על קלט קטן: r0/r1 נכונים, counters תואמים ל-`LocalPipelineTest`.
- [ ] **2. step2-rstats** – ✔ בדיקה עם דוגמת המטלה (N_1^0=3, T_1^01=4, N_1^1=3, T_1^10=2) כקלט מוכן.
- [ ] **3. step3-join** (secondary sort) – ✔ בדיקה עם 2 reducers: כל trigram מקבל N/T נכונים גם כשיש r=0.
- [ ] **4. step4-prob** – ✔ ערכים זהים ל-`ReferenceEstimator.estimate` על אותו קלט.
- [ ] **5. step5-sort** – ✔ פלט ממוין גלובלית עם ≥2 reducers (`CompareOutputs` לא מדווח "not sorted").
- [ ] **6. Driver מלא + קבלה** – להסיר `@Disabled` מ-`PairsAcceptanceTest`.
  ✔ `scripts/build.sh pairs` ירוק – 3 תרחישי הקבלה עוברים (text la/nola, sequence).
- [ ] **7. בדיקת מצבי local aggregation** – ✔ ב-`_stats` המקומי: `nola` → `combine_output_records = 0`; `la` → `reduce_input_records` קטן יותר.
- [ ] **8. EMR smoke** (זול) – `scripts/upload-test-input.sh`; `upload-jar.sh`; `launch-emr.sh pairs <id>-la-smoke true --input s3://<bucket>/shared/test-input.txt --input-format text --instance-count 1` (קודם בלי `--submit`).
  ✔ `fetch-results.sh --final` + `compare.sh` מול expected → OK.
- [ ] **9. EMR מלא – la ו-nola** (בתנאי החוזה §10).
  ✔ שתי הרצות הצליחו; `_stats` הורד; counters של הסינון זהים בין שתי ההרצות.
- [ ] **10. סטטיסטיקות** – `scripts/stats-table.sh` → `reports/pairs-statistics.md`.
  ✔ כל השדות בתבנית מולאו, כולל commit וזמני ריצה.
- [ ] **11. ניתוח** – 5 הזוגות שלך (החוזה §12), `scripts/top5.sh`, שיפוט.
  ✔ `reports/pairs-analysis.md` מלא; ההשלמות זהות לפלט של Stripes (אחרי שיהיה).
- [ ] **12. תיעוד** – Javadoc לכל מחלקה (תפקיד, key/value, זיכרון), והנחות הזיכרון בדוח.
  ✔ שותף ב' יכול להבין כל שלב מהתיעוד בלבד.
- [ ] **13. מיזוג** – PR מ-`feature/pairs` ל-`main` (ראו `GIT_WORKFLOW.md`).
  ✔ ה-PR ממוזג, `mvn clean verify` ב-`main` ירוק.

## 7. EMR וכסף

- תמיד הרצה יבשה קודם (בלי `--submit`), ובדיקה של ה-jar, ה-args והקידומת.
- ה-cluster נסגר מעצמו בסוף ה-step או בכשלון. ודאו ב-Console שאין cluster פעיל אחרי כל הרצה.
- אחרי איסוף התוצאות: מחקו את תיקיות `step*` (ביניים) מ-S3; השאירו `final/`, `_stats/`, `logs/` עד הציון.
- בהרצה המלאה הראשונה – עקבו אחרי ה-map tasks הראשונים (בעיית LZO אפשרית, `OPEN_QUESTIONS.md` #10).

## 8. תוצרים

`pairs/**` עם בדיקות ירוקות · `reports/pairs-statistics.md` · `reports/pairs-analysis.md` · נתיבי S3 של ההרצות המלאות (לשותף ב' ל-README הסופי).
