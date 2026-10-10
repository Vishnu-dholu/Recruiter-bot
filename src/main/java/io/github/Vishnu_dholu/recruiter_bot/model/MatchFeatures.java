package io.github.Vishnu_dholu.recruiter_bot.model;

import java.util.List;

/**
 * Stores the matching features for a job-candidate pair.
 * Used as input for MatchScorer
 */
public record MatchFeatures(
        long jobId,
        long candidateId,
        List<String> matchedSkills,   // required skills the candidate has that the job requires
        int requiredSkillCount,       // Total skills the job requires
        int candidateYears,
        int minYears,                 // Minimum experience required
        Availability availability,
        List<String> sharedTraits,    // candidate traits that equal one of the job's culture keywords
        int cultureKeywordCount) {    // how many culture keywords the job has in total

    public enum Availability { IMMEDIATE, TWO_WEEKS, NOT_LOOKING }

    public MatchFeatures {
        matchedSkills = List.copyOf(matchedSkills);
        sharedTraits = List.copyOf(sharedTraits);
    }
}