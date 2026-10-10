-- =====================================================================
-- SQL Detective (Part 2): hiring_ops dataset, written for PostgreSQL.
-- The dataset is loaded unchanged by sql-detective/hiring_ops_setup.sql.
-- Run all answers with:
--   docker compose exec -T db psql -U recruiter -d hiring_ops < sql-detective/sql_detective.sql
-- =====================================================================


-- Q1. List all currently open job postings, with the name of the recruiter who owns each.
-- LEFT JOIN so that an open posting is still listed even if its recruiter row were missing.
SELECT jp.id          AS job_posting_id,
       jp.title,
       jp.department,
       jp.opened_date,
       r.name         AS recruiter
FROM job_postings jp
         LEFT JOIN recruiters r ON r.id = jp.recruiter_id
WHERE jp.status = 'open'
ORDER BY jp.id;


-- Q2. For each job posting, how many applicants reached the Final interview stage?
-- "Reached" means a Final interview exists for them, whatever its outcome.
-- Trap 1: the Final filter sits in the JOIN condition, not in WHERE. A WHERE filter would
--         drop postings with no Final interview (5 and 6) instead of showing them with 0.
-- Trap 2: COUNT(DISTINCT applicant_id) counts people, not interview rows.
SELECT jp.id   AS job_posting_id,
       jp.title,
       jp.status,
       COUNT(DISTINCT i.applicant_id) AS applicants_at_final
FROM job_postings jp
         LEFT JOIN interviews i
                   ON i.job_posting_id = jp.id
                       AND i.stage = 'Final'
GROUP BY jp.id, jp.title, jp.status
ORDER BY jp.id;


-- Q3. Find every person who effectively applied to more than one job posting.
-- There is no applications table, so "applied to a posting" is read from the interviews table.
-- Trap 1: the same person can have several applicant rows (Ananya Rao, ids 1 and 2, emails differ
--         only in capitalisation). Grouping by applicant id would miss her, so people are
--         identified by lower(trim(email)).
-- Trap 2: count DISTINCT postings, not interviews. Someone with three interviews for one
--         posting has applied to one posting, not three.
SELECT lower(trim(a.email))                                    AS email,
       min(a.full_name)                                        AS full_name,
       COUNT(DISTINCT i.job_posting_id)                        AS postings_applied_to,
       array_agg(DISTINCT i.job_posting_id ORDER BY i.job_posting_id) AS job_posting_ids,
       array_agg(DISTINCT a.id ORDER BY a.id)                  AS applicant_row_ids
FROM applicants a
         JOIN interviews i ON i.applicant_id = a.id
GROUP BY lower(trim(a.email))
HAVING COUNT(DISTINCT i.job_posting_id) > 1
ORDER BY full_name;


-- Q4. For each recruiter, the Final-stage conversion rate (passed / total Final-stage interviews),
--     only for recruiters with at least 3 Final-stage interviews.
-- Trap: integer division. In PostgreSQL, 3 / 4 is 0. The ::numeric cast and 100.0 force a decimal result.
-- Every Final interview counts in the total, including any that are pending (outcome IS NULL).
SELECT r.id   AS recruiter_id,
       r.name AS recruiter,
       COUNT(*)                                              AS final_interviews,
       COUNT(*) FILTER (WHERE i.outcome = 'passed')          AS passed,
    ROUND(COUNT(*) FILTER (WHERE i.outcome = 'passed')::numeric / COUNT(*), 4) AS conversion_rate,
       ROUND(100.0 * COUNT(*) FILTER (WHERE i.outcome = 'passed') / COUNT(*), 2)  AS conversion_pct
FROM interviews i
         JOIN job_postings jp ON jp.id = i.job_posting_id
         JOIN recruiters r    ON r.id = jp.recruiter_id
WHERE i.stage = 'Final'
GROUP BY r.id, r.name
HAVING COUNT(*) >= 3
ORDER BY conversion_rate DESC, r.id;


-- Q5. (Bonus) The recruiter with the most successful placements (passed at Final stage) per department.
-- A placement is one passed Final interview. The window function ranks recruiters inside each
-- department. RANK (not ROW_NUMBER) is used so that recruiters tied for first all appear.
-- Departments with no placements at all (Infrastructure) have no rows to rank, so they are absent.
WITH placements AS (
    SELECT jp.department,
           r.id   AS recruiter_id,
           r.name AS recruiter,
           COUNT(*) AS placements
    FROM interviews i
             JOIN job_postings jp ON jp.id = i.job_posting_id
             JOIN recruiters r    ON r.id = jp.recruiter_id
    WHERE i.stage = 'Final'
      AND i.outcome = 'passed'
    GROUP BY jp.department, r.id, r.name
),
     ranked AS (
         SELECT department, recruiter_id, recruiter, placements,
                RANK() OVER (PARTITION BY department ORDER BY placements DESC) AS dept_rank
         FROM placements
     )
SELECT department, recruiter_id, recruiter, placements
FROM ranked
WHERE dept_rank = 1
ORDER BY department, recruiter_id;