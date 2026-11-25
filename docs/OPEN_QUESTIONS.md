# שאלות פתוחות והנחות

דרישות במטלה שאינן חד-משמעיות. לכל אחת: מה כתוב, מה ההחלטה הזמנית שלנו (כבר ממומשת בתשתית), ולמי לפנות.
מומלץ לשלוח את שאלות 1–4 ל-Meni (שאלות מושגיות) ואת 5 ל-Idan (טכנית). **עדכנו כאן כל תשובה שמתקבלת.**

| # | נושא | מה כתוב במטלה | ההחלטה שלנו (זמנית) | השפעה אם תשתנה |
|---|---|---|---|---|
| 1 | איזה r בנוסחה | `P_del = (T_r^01+T_r^10) / (N(N_r^0+N_r^1))`, "where C(w1...wn) = r" – r אחד | r0 (ספירה בחלק 0) לאיברי `^0`/`^01`, ו-r1 (ספירה בחלק 1) לאיברי `^1`/`^10`, לפי Manning & Schütze 6.2.4. r = 0 מוגדר על עולם ה-types שנראו בקורפוס | רק שלב החיבור בין ה-trigram לטבלאות; `DeletedEstimation` לא משתנה; `ReferenceEstimator` + expected ישתנו |
| 2 | מותנית או משותפת | הפלט: `P(w1w2w3)`; הדוגמה (0.6/0.4 לאותו w1w2) נראית כמו הסתברות מותנית | מוציאים את `P_del(w1w2w3)` כפי שהנוסחה מגדירה (משותפת). ערכי הדוגמה – להמחשת הפורמט והמיון בלבד | שלב נוסף (נרמול לפי w1w2) – בשני המימושים |
| 3 | "bigram" בסינון stop words | "remove all bigram that contain stop words" | מסירים כל **trigram** שאחת ממילותיו stop word; case-insensitive; לפני כל ספירה, כולל N | `CorpusFilter` בלבד |
| 4 | רשימת stop words | "provided in the assignment archive" | הארכיון לא היה זמין – `data/stopwords.txt` היא רשימה כללית זמנית. **להחליף לפני הרצות EMR**, להריץ `scripts/make-expected.sh` ולעשות commit | expected output משתנה; הקוד לא |
| 5 | גרסאות | Hadoop 2.6.0 ו-SDK v1 בדוגמה, "you may choose any other version" | Hadoop 3.4.1 (provided), EMR `emr-7.12.0`, Java 11 bytecode, AWS SDK v2. `m5.xlarge` במקום `m4.large` (EMR 7 מתעד את m5.xlarge כקטן ביותר מסוג general purpose) | `pom.xml` + `config/emr.properties` בלבד |
| 6 | מספר זוגות לניתוח | "Choose 10 'interesting' word pairs" | 5 לכל שותף = 10 שונים, מאוחדים בדוח הסופי | חלוקה בלבד |
| 7 | "Hsum" ב-Stripes | "the reduce method can store the Hsum table in memory" | פרשנות: טבלת הסכומים r → (N_r, T_r) (וגם ה-stripe של זוג w1w2 בודד) יכולים להיות בזיכרון ה-reducer ב-Stripes. **לעולם לא** טבלה r → רשימת n-grams | תכנון Stripes בלבד |
| 8 | ניקוי טוקנים | לא מוזכר | אין נרמול case ואין הסרת פיסוק (`Coffee` ≠ `coffee`; טוקנים כמו `,` נשארים) | `CorpusFilter` + expected |
| 9 | שובר שוויון במיון | רק w1w2 עולה והסתברות יורדת | w3 עולה (פלט דטרמיניסטי) | `OutputLine.ORDER` |
| 10 | LZO על EMR 7 | "sequence file format with block level LZO compression" | צריך לוודא ש-`emr-7.12.0` קורא את הקובץ. **הרצת ה-smoke הראשונה על הקורפוס האמיתי צריכה להיות קצרה** (מספר מכונות מינימלי) ולבדוק שאין `Unknown codec`. אם יש – לעבור ל-release ישן יותר שכולל hadoop-lzo, או להוסיף את ה-codec | `config/emr.properties` |

## הנחות נוספות שבוצעו בתשתית

- חלוקת הקורפוס לפי שורה (ngram+שנה) עם CRC32 – ראו `SHARED_CONTRACT.md` §3.
- `match_count ≤ 0`, שדה לא מספרי, או פחות מ-3 שדות → שורה פגומה, נספרת ב-`MALFORMED` ולא מפילה את ה-Job.
- פורמט ההסתברות `%.10e` – 11 ספרות משמעותיות, מספיק להבחנה בין ערכים ולהשוואה בין המימושים.
- דחיסת map output כבויה בכל ההרצות כדי שגודל ה-shuffle יהיה בר-השוואה (מייקר מעט את הרשת).
- ה-Repository הקיים שלכם לא היה נגיש בזמן הבנייה; הקבצים נוצרו מאפס. בהעתקה – בדקו התנגשויות עם קבצים קיימים (`README.md`, `.gitignore`).
