# Recruiter Bot

A small backend that takes candidate profiles and job openings, scores every sensible
candidate/job pair, and returns ranked matches in both directions. I built it for the
RecruiterFlow backend take-home. The data is the fictional cast from the brief, so
Sherlock Holmes is applying to be a Backend Detective.

The project has two parts. **Part 1** is the Spring Boot service. **Part 2** is the SQL Detective challenge. 
Its answers are in `sql-detective/sql_detective.sql`, and `sql-detective/hiring_ops_setup.sql` loads the dataset they run on. It does not depend on the service.

## Tech stack

| Layer | Choice |
|---|---|
| Language and framework | Java 21, Spring Boot 4.1 (Spring Web) |
| Database | PostgreSQL 17 |
| Database access | Spring `JdbcClient` with hand-written SQL (no ORM, no query builder) |
| Schema and seed data | Flyway, running hand-written `.sql` migrations |
| Build and run | Maven wrapper, Docker Compose |
| Tests | JUnit 6 |

## Running the project

### Before you start

- **Docker with Docker Compose** (the `docker compose` command). You need it for both ways of running the project.
- **JDK 21 or newer.** Only needed if you run the Java app (Option B) or the unit tests on your own machine. 
- You do not need to install Maven, because the project includes the Maven wrapper (`./mvnw`).

Then clone the repo:

```bash
git clone https://github.com/Vishnu-dholu/recruiter-bot.git
cd recruiter-bot
```

### Ports and credentials

These are for local use only.

| What | Value |
|---|---|
| The API | `http://localhost:8080` |
| PostgreSQL, from your machine | `localhost:5433` |
| PostgreSQL, inside Docker | port `5432` (host name `db`) |
| Username / password | `recruiter` / `recruiter` |
| Databases | `recruiter_bot` (the service) and `hiring_ops` (Part 2) |

PostgreSQL listens on 5432 inside its container. Docker publishes it on port 5433 of your machine, 
in both options, so it does not clash with a PostgreSQL you may already run on 5432. The app container 
talks to the database over Docker's internal network at `db:5432`. Only the Java app in Option B connects 
through `localhost:5433`. Port 5433 must be free on your machine either way.

### Option A: run everything in Docker

This is the easiest way, and it is what I would use to review the project.

```bash
docker compose up --build
```

This builds the app image and starts both PostgreSQL and the app. The first build takes a few minutes
because Maven downloads all the dependencies. Later builds are quick.

To stop it, press `Ctrl+C`, then run:

```bash
docker compose down
```

This removes the containers but keeps the database data. To wipe the database and start fresh, run:

```bash
docker compose down -v
```

The `-v` also deletes the data volume, which holds both databases. The next `docker compose up --build` 
recreates them and runs the migrations again.

### Option B: run the Java app on your machine, with PostgreSQL in Docker

This is handier while developing. Start only the database, in the background:

```bash
docker compose up -d db
```

Then start the app:

```bash
./mvnw spring-boot:run
```

The app connects to the database on `localhost:5433` (see `src/main/resources/application.properties`).
Stop the app with `Ctrl+C`. Stop the database with `docker compose down`, or use `docker compose down -v` to reset it.

### Check that it started

Watch the app logs. When everything is fine you will see these two lines:

```
Started RecruiterBotApplication
Computed 31 matches from 31 candidate/job pairs
```

You can also check the `matches` table. It should have 31 rows:

```bash
docker compose exec db psql -U recruiter -d recruiter_bot -c "SELECT count(*) FROM matches;"
```

### Try the API

There are two endpoints:

| Endpoint | What it returns |
|---|---|
| `GET /jobs/{id}/matches` | Candidates for a job, best match first |
| `GET /candidates/{id}/matches` | Jobs for a candidate, best match first |

An unknown id returns `404`. An id that is not a number returns `400`.

```bash
curl http://localhost:8080/jobs/5/matches          # Incident Commander: Olivia 90, Katniss 75, Dwight 50 ...
curl http://localhost:8080/candidates/12/matches   # Olivia: Incident Commander 90, then two jobs at 45
curl http://localhost:8080/jobs/3/matches          # a three-way tie, broken by experience
curl -i http://localhost:8080/jobs/99/matches      # 404
```

Each result looks like this. For the jobs
endpoint, `id` and `name` belong to the candidate. For the candidates endpoint, they belong to the job.

```json
{
  "rank": 1,
  "id": 12,
  "name": "Olivia P.",
  "score": 90.0,
  "breakdown": { "skills": 1.0, "experience": 1.0, "availability": 0.5, "culture": 0.5 },
  "reason": "3/3 required skills (crisis-management, negotiation, strategy); 13 yrs vs 4 required; available in 2 weeks; culture match: decisive"
}
```

The ids come from the order of the brief.
### Job IDs

| ID | Job Title |
|---:|---|
| 1 | Backend Detective |
| 2 | Rapid Prototyping Engineer |
| 3 | Developer Relations Lead |
| 4 | Engineering Manager (Chaos Team) |
| 5 | Incident Commander |
| 6 | Sales Engineer |

### Candidate IDs

| ID | Candidate Name |
|---:|---|
| 1 | Sherlock |
| 2 | Hermione |
| 3 | Tony |
| 4 | Leslie |
| 5 | Ron |
| 6 | Rick |
| 7 | Elle |
| 8 | MacGyver |
| 9 | Sheldon |
| 10 | Katniss |
| 11 | Michael |
| 12 | Olivia |
| 13 | Ted |
| 14 | Miranda |
| 15 | Dwight |


### Run the unit tests

```bash
./mvnw test -Dtest=MatchScorerTest
```

This runs the 10 scorer tests. They do not need the database. They check the worked examples below, partial
skills, no skill overlap, experience below the minimum, over-qualified candidates, not-looking candidates, and
some edge cases (a minimum of 0, a job with no culture keywords). One test also checks that a perfect candidate
scores exactly 100, which proves the weights add up.

## How the matching works

Every candidate/job pair gets a score from 0 to 100. The score has four parts:

| Part | Points | How it is worked out |
|---|---|---|
| Skills | 60 | Required skills the candidate has, divided by the skills the job requires |
| Experience | 20 | Full points if the candidate meets the job's minimum, otherwise years divided by the minimum |
| Availability | 10 | Immediately 1.0, in two weeks 0.5, not looking 0 |
| Culture | 10 | Candidate traits that match the job's culture keywords, divided by the job's keyword count |

So the score is `60 × skills + 20 × experience + 10 × availability + 10 × culture`, where each part is a
number from 0 to 1. For example, Olivia for Incident Commander: she has 3 of the 3 required skills (60), her
13 years beat the minimum of 4 (20), she is available in two weeks (half of 10, so 5), and she matches one of
the two culture keywords, "decisive" (half of 10, so 5). That adds up to **90**.

**Skills count the most** because they are the best sign that someone can do the job. Culture is the weakest signal: 
only nine trait/keyword pairs match in the seed data, so it gets 10 points and mostly helps break ties.

**A candidate must share at least one required skill to appear.** Otherwise, someone with no relevant skill could still 
earn up to 40 points from experience, availability and culture. With this rule, 31 of the 90 possible pairs remain, 
and every candidate and job still has at least one match.

**Candidates who are "not looking" stay in the results.** Availability is only 10 points, so a strong candidate who is 
not looking can still outrank a weaker available one (Miranda ranks first for Engineering Manager). 
I did this on purpose: hiding them throws away useful information, and the `reason` text says "not currently looking" 
so the recruiter can decide. The trade-off is that the ranking is not a list of people you can hire today. An `availableOnly` filter would be a simple next step.

**Experience** gets full points once the minimum is met, and a proportional share below it (2 years against 4 gives half). 
Over-qualified candidates are not penalised, because the brief says "minimum". The seed data never has anyone below a minimum, 
so that rule is covered only by the unit tests. **Ties** are settled by higher skill coverage, then more years of experience, 
then the lower id, so the order is the same on every run.

### How It Is Built

The matching process starts with an SQL query that finds candidate/job pairs sharing at least one required skill. 
It uses joins and indexes on `skill_id` instead of comparing every candidate with every job. The results are then 
passed to `MatchScorer`, a simple Java class that calculates the match score and explains why the candidate matches 
the job. Since this class does not depend on the database or Spring, I can test the scoring rules separately.

The calculated matches are stored in the `matches` table rather than being recalculated every time someone calls the API. 
When the application starts, it deletes the existing matches and rebuilds them inside a single database transaction. 
If something goes wrong, the transaction rolls back, keeping the previous results intact. This also prevents duplicate matches when the application restarts.

When an API request comes in, the application retrieves the stored matches using database indexes. SQL's `ROW_NUMBER()` 
function assigns a rank to each result based on the sorting rules. I wrote the SQL queries manually in `MatchRepository` 
and execute them using Spring's `JdbcClient`, which handles database connections and query parameters and runs my 
row-mapping code that turns each row into a Java object. Flyway manages the database schema and seed data through SQL migration files.


## Limitations and Future Improvements

Currently, candidates and jobs are loaded through database migrations, and matches are recalculated only at startup. 
I would add `POST` endpoints to create or update records and recalculate only the affected matches.

Skills are matched by exact name, so `public-speaking` and `presenting` are treated as different skills. 
A synonym table or normalization could improve this. Culture matching is basic, and the scoring weights are fixed in the code; both could be made more flexible.

Recalculating matches at startup works for the current dataset. For larger datasets, I would update matches incrementally 
and add pagination to API responses. I tested the repository and controller manually using Postman and `psql`. 
Integration tests with PostgreSQL using Testcontainers would provide more thorough testing.

## Database schema

The schema is in `src/main/resources/db/migration/V1__schema.sql`.

| Table | What it holds |
|---|---|
| `candidates` | name, years of experience, availability (`IMMEDIATE`, `TWO_WEEKS`, `NOT_LOOKING`), quirk |
| `jobs` | title, minimum experience, tagline |
| `skills` | the shared list of skills, lowercase and unique |
| `candidate_skills`, `job_required_skills` | links between candidates or jobs and `skills`, indexed both ways |
| `candidate_traits`, `job_culture_keywords` | plain lowercase text, compared directly |
| `matches` | one row per matching pair: the score, the four part scores and the reason |

Foreign keys, `CHECK` constraints and `ON DELETE CASCADE` keep the data consistent. The `matches` table has an
index for each endpoint, on the id and the score (highest first).

The seed data is loaded by `V2__seed_data.sql`. It links skills, traits and keywords by name. A check at the
end counts the rows, so a typo makes the migration fail instead of silently dropping data.

## Part 2: SQL Detective

The answers are in `sql-detective/sql_detective.sql`, one query per question. Each one has a `-- Q#` comment above
it. The dataset from the brief is loaded as it is, into a separate `hiring_ops` database. The script
`sql-detective/hiring_ops_setup.sql` does that, and Postgres runs it automatically the first time the database
volume is created. If `hiring_ops` is missing, reset with `docker compose down -v` and start again. 
This deletes the whole database volume, so both `recruiter_bot` and `hiring_ops` are wiped. That is fine here, 
because the next start rebuilds both from the seed files, but don't use it if you have added data of your own.

To run all the answers:

```bash
docker compose exec -T db psql -U recruiter -d hiring_ops < sql-detective/sql_detective.sql
```

This feeds the file into `psql` and prints one result set per question. The data has a few traps:

- **Q2 (applicants who reached the Final stage, per posting):** the Final filter is in the `LEFT JOIN ... ON`
  clause, not in `WHERE`. A `WHERE` filter would make postings 5 and 6 vanish. This way they show 0. It counts
  distinct applicants, not interview rows.
- **Q3 (people who applied to more than one posting):** I identify a person by `lower(trim(email))`. Ananya Rao has
  two applicant rows, and their emails differ only in capital letters. I also count distinct postings, not
  interviews, so several interviews for one posting do not look like several applications.
  The result is Ananya Rao, Ben Turner and Elena Popescu.
- **Q4 (Final-stage conversion rate):** I cast to `numeric`. Without it Postgres does integer division and every
  rate is 0. The result is Priya Shah 0.75 and Daniel Ortiz 0.50. Fatima has only one Final interview, so she is
  left out.
- **Q5 (bonus, top recruiter per department):** I use `RANK()` inside each department, so recruiters who tie for
  first all appear. The result is Priya Shah for Engineering (3 placements) and Fatima Al-Sayed for Data (1).
  Infrastructure has no placements, so it does not appear.
- **Q1** lists the four open postings (1, 2, 4 and 5) with their recruiters.

I made three assumptions. "Applied to a posting" means having an interview for it, because there is no
applications table. "Reached Final" means a Final interview exists, whatever its outcome. A placement is one
passed Final interview.

## Project structure


```
recruiter-bot/
├── README.md                          # setup steps, API examples, design decisions, AI note
├── docker-compose.yml                 # starts PostgreSQL and the app (database on host port 5433)
├── Dockerfile                         # two stages: Maven builds the jar, a slim Java 21 image runs it
├── pom.xml                            # Maven dependencies and build settings
├── mvnw, mvnw.cmd, .mvn/              # Maven wrapper, so Maven does not need to be installed
├── .gitignore                         # files Git skips (for example target/)
│
├── sql-detective/                     # Part 2, independent of the service
│   ├── hiring_ops_setup.sql           # creates the hiring_ops database and loads the brief's data as given
│   └── sql_detective.sql              # the answers, Q1 to Q5
│
└── src/
    ├── main/
    │   ├── java/io/github/Vishnu_dholu/recruiter_bot/
    │   │   ├── RecruiterBotApplication.java   # app entry point
    │   │   ├── controller/
    │   │   │   └── MatchController.java       # the two endpoints; returns 404 for unknown ids
    │   │   ├── service/
    │   │   │   ├── MatchService.java          # rebuilds all matches at startup, serves the ranked lists
    │   │   │   └── MatchScorer.java           # scoring formula and reason text (plain Java, no database)
    │   │   ├── repository/
    │   │   │   └── MatchRepository.java       # all the hand-written SQL, run through JdbcClient
    │   │   ├── model/
    │   │   │   └── MatchFeatures.java         # the facts about one candidate/job pair
    │   │   └── dto/
    │   │       └── MatchResult.java           # the JSON shape the API returns
    │   └── resources/
    │       ├── application.properties         # database connection settings
    │       └── db/migration/                  # Flyway migrations, run in order at startup
    │           ├── V1__schema.sql             # tables, constraints and indexes
    │           └── V2__seed_data.sql          # the 15 candidates and 6 jobs, with row-count checks
    └── test/
        └── java/io/github/Vishnu_dholu/recruiter_bot/service/
            └── MatchScorerTest.java           # 10 unit tests for the scoring rules
```

## How I used AI

I used Claude (Anthropic) as a coding assistant to plan the project structure, discuss database schema and scoring 
options, and help draft Java code, SQL queries, Docker configuration, and unit tests.

I made the key design decisions, including the 60/20/10/10 scoring weights and keeping candidates who are not currently 
looking in the results. I also ran the application, debugged issues, and checked the SQL query results against the dataset. 
During testing, a unit test exposed a rounding bug in the scoring logic, which I investigated and fixed.