-- V1: core schema for Recruiter Bot (hand-written DDL, run by Flyway on startup).
-- Rule: once this has run against a database, never edit it. Add V3__... instead,
-- because Flyway stores a checksum of every applied file and refuses to start on a mismatch.

-- ---------- Core entities ----------

CREATE TABLE candidates (
                            id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            name             TEXT    NOT NULL UNIQUE,   -- UNIQUE gives re-ingestion a key for ON CONFLICT upserts
                            experience_years INTEGER NOT NULL CHECK (experience_years >= 0),
                            availability     TEXT    NOT NULL CHECK (availability IN ('IMMEDIATE', 'TWO_WEEKS', 'NOT_LOOKING')),
                            quirk            TEXT                       -- flavour text, returned in responses but not scored
);

CREATE TABLE jobs (
                      id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      title          TEXT    NOT NULL UNIQUE,
                      min_experience INTEGER NOT NULL CHECK (min_experience >= 0),
                      tagline        TEXT
);

-- ---------- Skills: a lookup table, so overlap is an integer join (no string splitting) ----------

CREATE TABLE skills (
                        id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE CHECK (name = lower(name))   -- lowercase rule keeps equality matching reliable
);

CREATE TABLE candidate_skills (
                                  candidate_id BIGINT NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
                                  skill_id     BIGINT NOT NULL REFERENCES skills (id),
                                  PRIMARY KEY (candidate_id, skill_id)
);
CREATE INDEX idx_candidate_skills_skill ON candidate_skills (skill_id);   -- skill -> candidates direction

CREATE TABLE job_required_skills (
                                     job_id   BIGINT NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
                                     skill_id BIGINT NOT NULL REFERENCES skills (id),
                                     PRIMARY KEY (job_id, skill_id)
);
CREATE INDEX idx_job_required_skills_skill ON job_required_skills (skill_id);

-- ---------- Traits and culture keywords: plain text, compared directly ----------

CREATE TABLE candidate_traits (
                                  candidate_id BIGINT NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
                                  trait        TEXT   NOT NULL CHECK (trait = lower(trait)),
                                  PRIMARY KEY (candidate_id, trait)
);

CREATE TABLE job_culture_keywords (
                                      job_id  BIGINT NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
                                      keyword TEXT   NOT NULL CHECK (keyword = lower(keyword)),
                                      PRIMARY KEY (job_id, keyword)
);

-- ---------- Matches: one stored row per (job, candidate) pair ----------

CREATE TABLE matches (
                         job_id             BIGINT       NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
                         candidate_id       BIGINT       NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
                         score              NUMERIC(5,2) NOT NULL CHECK (score BETWEEN 0 AND 100),
                         skill_score        NUMERIC(4,3) NOT NULL CHECK (skill_score        BETWEEN 0 AND 1),
                         experience_score   NUMERIC(4,3) NOT NULL CHECK (experience_score   BETWEEN 0 AND 1),
                         culture_score      NUMERIC(4,3) NOT NULL CHECK (culture_score      BETWEEN 0 AND 1),
                         availability_score NUMERIC(4,3) NOT NULL CHECK (availability_score BETWEEN 0 AND 1),
                         reason             TEXT         NOT NULL,
                         computed_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         PRIMARY KEY (job_id, candidate_id)
);
-- Each endpoint reads "all rows for one id, best score first", so each gets a matching index.
CREATE INDEX idx_matches_job_rank       ON matches (job_id, score DESC);
CREATE INDEX idx_matches_candidate_rank ON matches (candidate_id, score DESC);