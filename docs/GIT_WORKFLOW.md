# עבודה עם Git – שני ענפים, בלי לחכות אחד לשני

## עיקרון

- `main` מכיל את התשתית המשותפת. מסמנים אותה ב-tag (`common-v1`).
- כל שותף עובד רק בענף שלו ורק בתיקיות שלו → **אין קונפליקטים** במיזוג.
- `common/`, `pom.xml`, `data/`, `scripts/`, `emr-launcher/`, `config/*.example`, `docs/SHARED_CONTRACT.md` – משתנים רק ב-`main`, בהסכמה.

| שותף | ענף | תיקיות/קבצים |
|---|---|---|
| א' | `feature/pairs` | `pairs/**`, `reports/pairs-*.md`, `docs/PAIRS_TASKS.md` |
| ב' | `feature/stripes` | `stripes/**`, `reports/stripes-*.md`, `docs/STRIPES_TASKS.md`; בסוף: `README.md`, `reports/statistics.md`, `reports/analysis.md` |

## הקמה – פעם אחת (אחד מכם)

```bash
cd <your-repo>                       # ה-Repository הקיים
git switch main && git pull
# להעתיק את תוכן assignment2/ לשורש ה-Repository (או לתת-תיקייה, לפי המבנה אצלכם).
# אם כבר קיימים README.md / .gitignore – למזג ידנית, לא לדרוס.
git status                           # לוודא ש-target/, config/emr.properties, results/ לא מופיעים
mvn -B clean verify                  # חייב להיות ירוק לפני commit
git add .
git commit -m "Shared infrastructure: common, skeletons, contract, scripts"
git push origin main
git tag -a common-v1 -m "Shared infrastructure v1" && git push origin common-v1
git branch feature/pairs && git branch feature/stripes
git push -u origin feature/pairs feature/stripes
```

## התחלת עבודה

```bash
# שותף א'
git fetch && git switch feature/pairs
scripts/build.sh pairs               # common + pairs בלבד

# שותף ב'
git fetch && git switch feature/stripes
scripts/build.sh stripes             # common + stripes בלבד
```

עבודה שוטפת: commits קטנים, `git push` לענף שלך. לפני push: `scripts/build.sh pairs` (או `stripes`) ירוק.

## אם בכל זאת צריך לשנות את התשתית המשותפת

1. מסכמים ביניכם (זה משנה את החוזה לשניכם).
2. ענף קצר מ-`main` (למשל `fix/common-xyz`), שינוי + בדיקות, PR ל-`main`, review של השותף השני, merge.
3. tag חדש (`common-v2`), ועדכון המסמכים אם צריך.
4. כל שותף מושך לענף שלו:
   ```bash
   git fetch && git merge origin/main
   ```
5. אם `data/stopwords.txt` השתנה – `scripts/make-expected.sh` נעשה **באותו** commit ב-`main`.

## מיזוג בסיום

```bash
# כל שותף, כשכל הבדיקות שלו ירוקות:
git fetch && git merge origin/main   # לוודא שהענף מעודכן
scripts/build.sh all                 # כל המודולים
git push
# PR: feature/pairs → main  (review: השותף השני)
# PR: feature/stripes → main
```

אחרי ששני ה-PRs מוזגו – שותף ב' עובד על `main` (או `feature/final`) לאיחוד הדוחות, README הסופי והאריזה (`STRIPES_TASKS.md` §8).

## לעולם לא ב-git

`config/emr.properties`, מפתחות AWS, `*.pem`, `target/`, jars, `results/`, `local-runs/` – כבר ב-`.gitignore`. לפני כל commit: `git status`.
