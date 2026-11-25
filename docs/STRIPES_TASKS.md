# משימות שותף ב' – מימוש Stripes + איחוד והגשה

ענף: `feature/stripes` · מודול: `stripes/` · Driver: `dsp.a2.stripes.StripesDriver`
קראו קודם: `docs/SHARED_CONTRACT.md` ו-`docs/OPEN_QUESTIONS.md`.

## 1. תחום האחריות

| מותר לשנות | לא משנים (משותף – רק דרך `main` בהסכמה) |
|---|---|
| `stripes/**` | `common/**`, `pom.xml` (שורש), `data/**` |
| `reports/stripes-statistics.md`, `reports/stripes-analysis.md` | `emr-launcher/**`, `scripts/**`, `config/*.example` |
| סימון התקדמות במסמך הזה | `docs/SHARED_CONTRACT.md`, `pairs/**` |
| **בסוף בלבד** (אחרי ששני הענפים מוזגו): `README.md`, `reports/statistics.md`, `reports/analysis.md` | |

עזר שרק Stripes משתמש בו → בתוך `stripes/`, לא ב-`common`.

## 2. מה כבר קיים – ובמה להשתמש

| מחלקה (`common`) | למה |
|---|---|
| `RunParameters` | פענוח שורת הפקודה (כבר מחובר ב-`StripesDriver`) |
| `JobSetup` | `apply` (כבר נקרא), `configureCorpusInput`, `configureReducers`, `loadStopWords`, `localAggregation`, `setCorpusN` / `corpusN` |
| `CorpusFilter` + `CommonCounters.record` | **חובה** ב-Mapper הראשון: פענוח, סינון, part, counters |
| `NGramRecord`, `NGramParser`, `StopWords`, `CorpusSplitter` | בשימוש דרך `CorpusFilter` |
| `DeletedEstimation.probability` | **חובה** לחישוב ההסתברות |
| `OutputLine` / `OutputSortKey` | פורמט וסדר הפלט הסופי |
| `OutputLayout` | נתיבי `step<K>-<name>`, `final`, `_stats` |
| `StepRunner.run` | **חובה** להרצת כל שלב (סטטיסטיקות) |
| `LocalHadoop`, `TextToSequenceFile` | בדיקות מקומיות |
| `ReferenceEstimator`, `CompareOutputs`, `TopCompletions`, `StatsTable` | expected, השוואה, ניתוח, טבלאות |

קבצי בדיקה מוכנים: `stripes/src/test/java/dsp/a2/stripes/StripesAcceptanceTest.java` (כרגע `@Disabled`), `StripesDriverTest`, `ModuleIndependenceTest`.

## 3. מה עליך לממש

1. כל שלבי ה-MapReduce של Stripes תחת `dsp.a2.stripes`, כולל Writable ל-stripe.
2. שרשור השלבים ב-`StripesDriver.run`.
3. מצב עם ובלי local aggregation לפי `JobSetup.localAggregation(conf)`.
4. פלט ממוין **גלובלית** ב-`finalDir()`.
5. תיעוד המימוש והנחות הזיכרון.
6. **בנוסף:** איחוד הדוחות, README סופי, אריזת ההגשה (סעיף 8).

## 4. הנחות הזיכרון של Stripes

- המטלה מתירה: "the reduce method can store the **Hsum** table in memory". הפרשנות המוסכמת (`OPEN_QUESTIONS.md` #7): ב-reducer מותר להחזיק (א) את ה-stripe הממוזג של זוג `w1 w2` אחד, ו-(ב) את טבלת הסכומים r → (N_r, T_r).
- **אסור** בכל מקרה: טבלה r → רשימת n-grams; כל ה-stripes של הקורפוס בזיכרון.
- את ההנחות יש **להצדיק** בדוח: למשל גודל stripe מקסימלי (מספר ה-w3 השונים לזוג נפוץ אחרי סינון stop words), מספר ערכי r השונים מול זיכרון ה-container.
- מבני נתונים: `MapWritable` כבד (שומר שם מחלקה לכל ערך). עדיף Writable ייעודי (למשל מערכים ממוינים של w3 ומונים, או `HashMap<String,long[]>` עם סריאליזציה ידנית). זה משפיע ישירות על גודל ה-shuffle בסטטיסטיקה.
- local aggregation: Combiner שממזג stripes (סכום לפי איבר), ו/או in-mapper combining (שורות של אותו ngram בשנים שונות מגיעות ברצף בדאטה). in-mapper חייב flush כשהמבנה גדל.
- להימנע מזוגות key-value מיותרים.

## 5. תכנון מוצע לשלבים (הצעה – אתה מחליט ומצדיק)

| שלב | Map | Reduce | Local aggregation |
|---|---|---|---|
| `step1-count` | `CorpusFilter` → key=`w1 w2`, value=stripe {w3 → (c0,c1)} | מיזוג stripes → `w1 w2` + stripe {w3 → (r0,r1)} | Combiner ממזג stripes |
| `step2-rstats` | מכל stripe: H0 {r0 → (1, r1)} ו-H1 {r1 → (1, r0)} → key=part, value=H | Hsum לכל part → טבלה `part r N_r T_r` | Combiner ממזג H |
| `step3-prob` | key=`w1 w2`, value=stripe מ-step1 | `setup` טוען את Hsum (פלט step2) לזיכרון; לכל w3 → `DeletedEstimation.probability` עם `JobSetup.corpusN` | – |
| `step4-sort` | key=`OutputSortKey` | identity → `finalDir()` | – |

נקודות שחשוב להבין ולהסביר בהגנה:
- step2 עם 2 keys בלבד מרכז הכול ב-2 reducers – אבל אחרי Combiner יש מעט רשומות; להסביר ולמדוד.
- אפשרות אופטימיזציה: ב-step3, אם ה-partitioner של `w1 w2` הוא range (סדר גלובלי) וה-reducer ממיין את ה-stripe לפי הסתברות יורדת בזיכרון (מותר) – אפשר לוותר על step4. אם עושים – לתעד.
- טבלת Hsum נקראת מ-HDFS/S3 ב-`setup` (או distributed cache) – לא מועברת דרך Configuration.
- r0=0 / r1=0 – הסטטיסטיקה של r=0 (החוזה §4).

## 6. סדר עבודה וקריטריוני סיום

- [ ] **0. הכנה** – `git switch feature/stripes`; `scripts/build.sh stripes` עובר (3 עוברות, 3 מדולגות). ✔ build ירוק.
- [ ] **1. Stripe Writable** – ✔ בדיקות round-trip (סריאליזציה), מיזוג, stripe ריק, UTF-8.
- [ ] **2. step1-count** – ✔ בדיקת יחידה: r0/r1 נכונים; counters תואמים ל-`LocalPipelineTest`.
- [ ] **3. step2-rstats** – ✔ בדיקה עם דוגמת המטלה (N_1^0=3, T_1^01=4, N_1^1=3, T_1^10=2).
- [ ] **4. step3-prob** – ✔ ערכים זהים ל-`ReferenceEstimator.estimate`.
- [ ] **5. step4-sort** (או ויתור מתועד) – ✔ סדר גלובלי עם ≥2 reducers.
- [ ] **6. Driver מלא + קבלה** – להסיר `@Disabled` מ-`StripesAcceptanceTest`. ✔ `scripts/build.sh stripes` ירוק, 3 תרחישים עוברים.
- [ ] **7. מצבי local aggregation** – ✔ `nola` → `combine_output_records = 0`; `la` → פחות רשומות/בתים.
- [ ] **8. EMR smoke** – כמו אצל Pairs (`upload-test-input.sh`, `upload-jar.sh`, `launch-emr.sh stripes ...`). ✔ `compare.sh` מול expected → OK.
- [ ] **9. EMR מלא – la ו-nola** בתנאי החוזה §10. ✔ counters הסינון זהים ל-Pairs (`N_INSTANCES`, `KEPT_ROWS`...).
- [ ] **10. סטטיסטיקות** → `reports/stripes-statistics.md`. ✔ כל השדות מולאו.
- [ ] **11. ניתוח** – זוגות 6–10 (החוזה §12) → `reports/stripes-analysis.md`. ✔ מלא.
- [ ] **12. תיעוד** – Javadoc + הנחות זיכרון בדוח. ✔ שותף א' מבין כל שלב מהתיעוד.
- [ ] **13. מיזוג** – PR מ-`feature/stripes` ל-`main`. ✔ ממוזג, `main` ירוק.

## 7. EMR וכסף

ראו `PAIRS_TASKS.md` §7 – אותם כללים (הרצה יבשה קודם, סגירת clusters, ניקוי ביניים, מעקב LZO).

## 8. איחוד, README סופי והגשה (אחרי ששני הענפים מוזגו ל-`main`)

- [ ] **14. השוואת תקינות סופית** (החוזה §11): counters זהים; top-5 של 10 הזוגות זהה בשני הפלטים; אופציונלי `final-checksum.sh`.
  ✔ טבלת השוואה קצרה בדוח הסטטיסטיקה המאוחד.
- [ ] **15. `reports/statistics.md`** – איחוד שתי הטבלאות + השוואה Pairs מול Stripes, עם ובלי local aggregation, והסבר ההבדלים (מספר רשומות, בתים, מפתחות, זמן).
  ✔ כל 4 ההרצות × כל השלבים מופיעים.
- [ ] **16. `reports/analysis.md`** – 10 הזוגות, top-5 לכל אחד, שיפוט סביר/לא סביר.
- [ ] **17. README סופי** (אנגלית, לפי דרישות ההגשה): שמות, usernames, ת"ז; איך מריצים; תיאור שלבי MapReduce של שני המימושים; הנחות זיכרון; קישור ל-S3 של `final/` (או הפלט); הערות.
  ✔ שותף א' עבר על ה-README ואישר.
- [ ] **18. אריזה** – מ-`main` נקי: `git archive --format=zip -o assignment2-submission.zip HEAD` (מכיל מקורות בלי `target/` ובלי jars), ובדיקה שה-zip כולל README, `reports/`, מקורות.
  ✔ ה-zip נפתח ונבנה (`mvn clean verify`) בתיקייה זמנית.
- [ ] **19. ניקוי AWS** – אין clusters פעילים; נמחקו נתוני ביניים; נשאר רק מה שה-README מפנה אליו.
