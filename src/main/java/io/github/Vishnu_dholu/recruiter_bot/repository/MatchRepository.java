package io.github.Vishnu_dholu.recruiter_bot.repository;

import io.github.Vishnu_dholu.recruiter_bot.dto.MatchResult;
import io.github.Vishnu_dholu.recruiter_bot.model.MatchFeatures;
import io.github.Vishnu_dholu.recruiter_bot.model.MatchFeatures.Availability;
import io.github.Vishnu_dholu.recruiter_bot.service.MatchScorer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * The only class that talks to the database. Every statement is hand-written SQL;
 * JdbcClient just opens connections, binds the :named parameters and maps rows.
 */

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
@Repository
public class MatchRepository {

    // ---------- SQL ----------

    /**
     * The facts for every (job, candidate) pair that shares at least one required skill.
     *
     * It starts from the skill links and joins them together, instead of crossing all jobs with all
     * candidates, so only pairs that overlap are ever produced, and the skill_id indexes are used.
     */
    private static final String FIND_ALL_FEATURES = """
            WITH overlap AS (
                SELECT r.job_id,
                       cs.candidate_id,
                       array_agg(s.name ORDER BY s.name) AS matched_skills
                FROM job_required_skills r
                JOIN candidate_skills cs ON cs.skill_id = r.skill_id
                JOIN skills s            ON s.id = r.skill_id
                GROUP BY r.job_id, cs.candidate_id
            )
            SELECT o.job_id,
                   o.candidate_id,
                   o.matched_skills,
                   (SELECT count(*) FROM job_required_skills r WHERE r.job_id = o.job_id) AS required_skill_count,
                   c.experience_years,
                   j.min_experience,
                   c.availability,
                   ARRAY(SELECT k.keyword
                         FROM job_culture_keywords k
                         JOIN candidate_traits t ON t.trait = k.keyword
                         WHERE k.job_id = o.job_id AND t.candidate_id = o.candidate_id
                         ORDER BY k.keyword) AS shared_traits,
                   (SELECT count(*) FROM job_culture_keywords k WHERE k.job_id = o.job_id) AS culture_keyword_count
            FROM overlap o
            JOIN candidates c ON c.id = o.candidate_id
            JOIN jobs j       ON j.id = o.job_id
            ORDER BY o.job_id, o.candidate_id
            """;

    private static final String DELETE_ALL_MATCHES = "DELETE FROM matches";

    private static final String INSERT_MATCH = """
            INSERT INTO matches (job_id, candidate_id, score, skill_score, experience_score,
                                 availability_score, culture_score, reason)
            VALUES (:jobId, :candidateId, :score, :skillScore, :experienceScore,
                    :availabilityScore, :cultureScore, :reason)
            """;

    /**
     * Ranked candidates for one job. The rank is a window function over the tie-break order:
     * score, then skill coverage, then more years of experience, then lower id.
     */
    private static final String MATCHES_FOR_JOB = """
            SELECT ROW_NUMBER() OVER (ORDER BY m.score DESC, m.skill_score DESC,
                                               c.experience_years DESC, c.id) AS match_rank,
                   c.id   AS id,
                   c.name AS name,
                   m.score, m.skill_score, m.experience_score, m.availability_score, m.culture_score,
                   m.reason
            FROM matches m
            JOIN candidates c ON c.id = m.candidate_id
            WHERE m.job_id = :jobId
            ORDER BY match_rank
            """;

    /** Ranked jobs for one candidate: score, then skill coverage, then lower job id. */
    private static final String MATCHES_FOR_CANDIDATE = """
            SELECT ROW_NUMBER() OVER (ORDER BY m.score DESC, m.skill_score DESC, j.id) AS match_rank,
                   j.id    AS id,
                   j.title AS name,
                   m.score, m.skill_score, m.experience_score, m.availability_score, m.culture_score,
                   m.reason
            FROM matches m
            JOIN jobs j ON j.id = m.job_id
            WHERE m.candidate_id = :candidateId
            ORDER BY match_rank
            """;

    private static final String JOB_EXISTS = "SELECT EXISTS (SELECT 1 FROM jobs WHERE id = :id)";
    private static final String CANDIDATE_EXISTS = "SELECT EXISTS (SELECT 1 FROM candidates WHERE id = :id)";

    // ---------- Row mapping ----------

    private static final RowMapper<MatchFeatures> FEATURES_MAPPER = (rs, rowNum) -> new MatchFeatures(
            rs.getLong("job_id"),
            rs.getLong("candidate_id"),
            toList(rs.getArray("matched_skills")),
            rs.getInt("required_skill_count"),
            rs.getInt("experience_years"),
            rs.getInt("min_experience"),
            Availability.valueOf(rs.getString("availability")),   // same three values as the DB CHECK
            toList(rs.getArray("shared_traits")),
            rs.getInt("culture_keyword_count"));

    private static final RowMapper<MatchResult> RESULT_MAPPER = (rs, rowNum) -> new MatchResult(
            rs.getInt("match_rank"),
            rs.getLong("id"),
            rs.getString("name"),
            rs.getDouble("score"),
            new MatchResult.Breakdown(
                    rs.getDouble("skill_score"),
                    rs.getDouble("experience_score"),
                    rs.getDouble("availability_score"),
                    rs.getDouble("culture_score")),
            rs.getString("reason"));

    /** Converts a Postgres text[] column into a Java list. */
    private static List<String> toList(Array sqlArray) throws SQLException {
        try {
            return Arrays.asList((String[]) sqlArray.getArray());
        } finally {
            sqlArray.free();
        }
    }

    // ---------- Operations ----------

    private final JdbcClient jdbc;

    public MatchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Facts for every pair with at least one shared required skill (the input to MatchScorer). */
    public List<MatchFeatures> findAllFeatures() {
        return jdbc.sql(FIND_ALL_FEATURES).query(FEATURES_MAPPER).list();
    }

    public void deleteAllMatches() {
        jdbc.sql(DELETE_ALL_MATCHES).update();
    }

    public void insertMatch(long jobId, long candidateId, MatchScorer.Score score) {
        jdbc.sql(INSERT_MATCH)
                .param("jobId", jobId)
                .param("candidateId", candidateId)
                .param("score", score.total())
                .param("skillScore", score.skillScore())
                .param("experienceScore", score.experienceScore())
                .param("availabilityScore", score.availabilityScore())
                .param("cultureScore", score.cultureScore())
                .param("reason", score.reason())
                .update();
    }

    public List<MatchResult> findMatchesForJob(long jobId) {
        return jdbc.sql(MATCHES_FOR_JOB).param("jobId", jobId).query(RESULT_MAPPER).list();
    }

    public List<MatchResult> findMatchesForCandidate(long candidateId) {
        return jdbc.sql(MATCHES_FOR_CANDIDATE).param("candidateId", candidateId).query(RESULT_MAPPER).list();
    }

    public boolean jobExists(long jobId) {
        return jdbc.sql(JOB_EXISTS).param("id", jobId).query(Boolean.class).single();
    }

    public boolean candidateExists(long candidateId) {
        return jdbc.sql(CANDIDATE_EXISTS).param("id", candidateId).query(Boolean.class).single();
    }
}