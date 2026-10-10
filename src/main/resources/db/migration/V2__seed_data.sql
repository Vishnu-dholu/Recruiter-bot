-- V2: seed data (the ingestion step). Runs once, right after V1, on a fresh database.
-- Candidates and jobs are inserted in the brief's order, so ids are predictable:
-- candidates 1-15 and jobs 1-6 (Sherlock = 1, Backend Detective = 1, ...).
-- Skills, traits and keywords are linked by NAME, so no id is ever typed by hand.

-- ---------- Candidates ----------

INSERT INTO candidates (name, experience_years, availability, quirk) VALUES
                                                                         ('Sherlock H.',  8,  'IMMEDIATE',   'Solves problems by eliminating the impossible; occasionally insufferable in standups.'),
                                                                         ('Hermione G.',  4,  'TWO_WEEKS',   'Reads the entire documentation before writing a single line of code.'),
                                                                         ('Tony S.',      12, 'NOT_LOOKING', 'Ships an MVP overnight, refuses to write tests.'),
                                                                         ('Leslie K.',    6,  'IMMEDIATE',   'Has a binder for everything, including the binder.'),
                                                                         ('Ron S.',       15, 'NOT_LOOKING', 'Refuses to use more than one monitor.'),
                                                                         ('Rick S.',      20, 'IMMEDIATE',   'Solution works, mechanism deeply concerning.'),
                                                                         ('Elle W.',      3,  'IMMEDIATE',   'Underestimated in every standup, correct in every retro.'),
                                                                         ('MacGyver',     10, 'TWO_WEEKS',   'Fixes production outages with duct tape and a paperclip metaphor.'),
                                                                         ('Sheldon C.',   9,  'IMMEDIATE',   'Correct 95% of the time, insufferable 100% of the time.'),
                                                                         ('Katniss E.',   5,  'IMMEDIATE',   'Excellent under pressure, terrible with public speaking.'),
                                                                         ('Michael S.',   11, 'NOT_LOOKING', 'World''s best boss, according to a mug he bought himself.'),
                                                                         ('Olivia P.',    13, 'TWO_WEEKS',   'Handles it. Whatever it is.'),
                                                                         ('Ted L.',       7,  'IMMEDIATE',   'Turns every technical setback into a folksy metaphor.'),
                                                                         ('Miranda P.',   18, 'NOT_LOOKING', 'Reviews every PR personally. Says nothing. Everyone panics.'),
                                                                         ('Dwight S.',    9,  'IMMEDIATE',   'Assistant to the regional backend engineer.');

-- ---------- Jobs ----------

INSERT INTO jobs (title, min_experience, tagline) VALUES
                                                      ('Backend Detective',                3, 'We have a bug. We have no leads. We have you.'),
                                                      ('Rapid Prototyping Engineer',       2, 'Ship first, document never (kidding — please document).'),
                                                      ('Developer Relations Lead',         2, 'Explain complex things to confused humans, cheerfully.'),
                                                      ('Engineering Manager, Chaos Team',  5, 'Herd cats. The cats are senior engineers.'),
                                                      ('Incident Commander',               4, '3am page. You''re the one who picks up.'),
                                                      ('Sales Engineer',                   3, 'Sell the vision, then go build it.');

-- ---------- Skills (the shared vocabulary for candidates and jobs) ----------

INSERT INTO skills (name) VALUES
                              ('deduction'), ('pattern-recognition'), ('forensics'), ('research'), ('time-management'),
                              ('public-speaking'), ('systems-design'), ('rapid-prototyping'), ('leadership'),
                              ('project-management'), ('stakeholder-management'), ('woodworking'), ('minimalism'),
                              ('negotiation'), ('chemistry'), ('persuasion'), ('resourcefulness'), ('theoretical-analysis'),
                              ('precision'), ('strategy'), ('crisis-management'), ('sales'), ('team-building'),
                              ('optimism'), ('mentorship'), ('security'), ('loyalty');

-- ---------- Candidate skills ----------

INSERT INTO candidate_skills (candidate_id, skill_id)
SELECT c.id, s.id
FROM (VALUES
          ('Sherlock H.', 'deduction'), ('Sherlock H.', 'pattern-recognition'), ('Sherlock H.', 'forensics'),
          ('Hermione G.', 'research'), ('Hermione G.', 'time-management'), ('Hermione G.', 'public-speaking'),
          ('Tony S.', 'systems-design'), ('Tony S.', 'rapid-prototyping'), ('Tony S.', 'leadership'),
          ('Leslie K.', 'project-management'), ('Leslie K.', 'stakeholder-management'), ('Leslie K.', 'public-speaking'),
          ('Ron S.', 'woodworking'), ('Ron S.', 'minimalism'), ('Ron S.', 'negotiation'),
          ('Rick S.', 'systems-design'), ('Rick S.', 'rapid-prototyping'), ('Rick S.', 'chemistry'),
          ('Elle W.', 'persuasion'), ('Elle W.', 'research'), ('Elle W.', 'public-speaking'),
          ('MacGyver', 'rapid-prototyping'), ('MacGyver', 'resourcefulness'), ('MacGyver', 'chemistry'), ('MacGyver', 'systems-design'),
          ('Sheldon C.', 'theoretical-analysis'), ('Sheldon C.', 'pattern-recognition'), ('Sheldon C.', 'research'),
          ('Katniss E.', 'precision'), ('Katniss E.', 'strategy'), ('Katniss E.', 'crisis-management'),
          ('Michael S.', 'sales'), ('Michael S.', 'public-speaking'), ('Michael S.', 'team-building'),
          ('Olivia P.', 'crisis-management'), ('Olivia P.', 'negotiation'), ('Olivia P.', 'strategy'), ('Olivia P.', 'leadership'),
          ('Ted L.', 'team-building'), ('Ted L.', 'optimism'), ('Ted L.', 'mentorship'), ('Ted L.', 'public-speaking'),
          ('Miranda P.', 'leadership'), ('Miranda P.', 'negotiation'), ('Miranda P.', 'stakeholder-management'), ('Miranda P.', 'precision'),
          ('Dwight S.', 'sales'), ('Dwight S.', 'negotiation'), ('Dwight S.', 'security'), ('Dwight S.', 'loyalty')
     ) AS v (candidate_name, skill_name)
         JOIN candidates c ON c.name = v.candidate_name
         JOIN skills s     ON s.name = v.skill_name;

-- ---------- Candidate traits ----------

INSERT INTO candidate_traits (candidate_id, trait)
SELECT c.id, v.trait
FROM (VALUES
          ('Sherlock H.', 'analytical'), ('Sherlock H.', 'blunt'),
          ('Hermione G.', 'detail-oriented'), ('Hermione G.', 'overachiever'),
          ('Tony S.', 'confident'), ('Tony S.', 'innovative'),
          ('Leslie K.', 'tenacious'), ('Leslie K.', 'organized'),
          ('Ron S.', 'stubborn'), ('Ron S.', 'principled'),
          ('Rick S.', 'genius'), ('Rick S.', 'reckless'),
          ('Elle W.', 'optimistic'), ('Elle W.', 'sharp'),
          ('MacGyver', 'calm'), ('MacGyver', 'improviser'),
          ('Sheldon C.', 'rigid'), ('Sheldon C.', 'brilliant'),
          ('Katniss E.', 'resilient'), ('Katniss E.', 'decisive'),
          ('Michael S.', 'enthusiastic'), ('Michael S.', 'chaotic'),
          ('Olivia P.', 'decisive'), ('Olivia P.', 'intense'),
          ('Ted L.', 'empathetic'), ('Ted L.', 'persistent'),
          ('Miranda P.', 'demanding'), ('Miranda P.', 'decisive'),
          ('Dwight S.', 'intense'), ('Dwight S.', 'loyal')
     ) AS v (candidate_name, trait)
         JOIN candidates c ON c.name = v.candidate_name;

-- ---------- Job required skills ----------

INSERT INTO job_required_skills (job_id, skill_id)
SELECT j.id, s.id
FROM (VALUES
          ('Backend Detective', 'deduction'), ('Backend Detective', 'pattern-recognition'), ('Backend Detective', 'forensics'),
          ('Rapid Prototyping Engineer', 'rapid-prototyping'), ('Rapid Prototyping Engineer', 'systems-design'),
          ('Developer Relations Lead', 'public-speaking'), ('Developer Relations Lead', 'research'),
          ('Engineering Manager, Chaos Team', 'team-building'), ('Engineering Manager, Chaos Team', 'stakeholder-management'), ('Engineering Manager, Chaos Team', 'leadership'),
          ('Incident Commander', 'crisis-management'), ('Incident Commander', 'strategy'), ('Incident Commander', 'negotiation'),
          ('Sales Engineer', 'sales'), ('Sales Engineer', 'public-speaking'), ('Sales Engineer', 'negotiation')
     ) AS v (job_title, skill_name)
         JOIN jobs j   ON j.title = v.job_title
         JOIN skills s ON s.name = v.skill_name;

-- ---------- Job culture keywords ----------

INSERT INTO job_culture_keywords (job_id, keyword)
SELECT j.id, v.keyword
FROM (VALUES
          ('Backend Detective', 'analytical'), ('Backend Detective', 'autonomous'),
          ('Rapid Prototyping Engineer', 'innovative'), ('Rapid Prototyping Engineer', 'fast-paced'),
          ('Developer Relations Lead', 'energetic'), ('Developer Relations Lead', 'curious'),
          ('Engineering Manager, Chaos Team', 'empathetic'), ('Engineering Manager, Chaos Team', 'organized'),
          ('Incident Commander', 'decisive'), ('Incident Commander', 'calm-under-pressure'),
          ('Sales Engineer', 'enthusiastic'), ('Sales Engineer', 'persistent')
     ) AS v (job_title, keyword)
         JOIN jobs j ON j.title = v.job_title;

-- ---------- Safety net ----------
-- The joins above silently drop any row whose name has a typo. These checks make a
-- typo fail the migration loudly instead of leaving quietly incomplete data.

DO $$
BEGIN
    IF (SELECT count(*) FROM candidates)           <> 15 THEN RAISE EXCEPTION 'Seed check failed: expected 15 candidates'; END IF;
    IF (SELECT count(*) FROM jobs)                 <> 6  THEN RAISE EXCEPTION 'Seed check failed: expected 6 jobs'; END IF;
    IF (SELECT count(*) FROM skills)               <> 27 THEN RAISE EXCEPTION 'Seed check failed: expected 27 skills'; END IF;
    IF (SELECT count(*) FROM candidate_skills)     <> 50 THEN RAISE EXCEPTION 'Seed check failed: expected 50 candidate skills'; END IF;
    IF (SELECT count(*) FROM candidate_traits)     <> 30 THEN RAISE EXCEPTION 'Seed check failed: expected 30 candidate traits'; END IF;
    IF (SELECT count(*) FROM job_required_skills)  <> 16 THEN RAISE EXCEPTION 'Seed check failed: expected 16 job skills'; END IF;
    IF (SELECT count(*) FROM job_culture_keywords) <> 12 THEN RAISE EXCEPTION 'Seed check failed: expected 12 culture keywords'; END IF;
END $$;