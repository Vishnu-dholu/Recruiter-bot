package io.github.Vishnu_dholu.recruiter_bot.dto;

/**
 *  Represents a ranked job-candidate match.
 *  The ID and name refer to the candidate or job, depending on the endpoint.
 */
public record MatchResult(
        int rank,
        long id,
        String name,
        double score,
        Breakdown breakdown,
        String reason
) {
    public record Breakdown(double skills, double experience, double availability, double culture) {
    }
}
