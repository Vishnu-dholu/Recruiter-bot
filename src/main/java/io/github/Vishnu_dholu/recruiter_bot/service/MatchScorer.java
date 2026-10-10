package io.github.Vishnu_dholu.recruiter_bot.service;

import io.github.Vishnu_dholu.recruiter_bot.model.MatchFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns the facts about one (job, candidate) pair into a score from 0 to 100 and a short reason.
 *
 * score = 60 x skills + 20 x experience + 10 x availability + 10 x culture
 */

public class MatchScorer {
    // Weight add up to 100
    static final double SKILLS_WEIGHT = 60;
    static final double EXPERIENCE_WEIGHT = 20;
    static final double AVAILABILITY_WEIGHT = 10;
    static final double CULTURE_WEIGHT = 10;

    /** Stores the total score, individual score and reason */
    public record Score(
            double total,
            double skillScore,
            double experienceScore,
            double availabilityScore,
            double cultureScore,
            String reason
    ){
    }

    /**
     * Returns the match score
     * Returns empty if the job has no required skills or the candidate has no matching skills.
     */
    public Optional<Score> score(MatchFeatures f){
        if(f.requiredSkillCount() <= 0 || f.matchedSkills().isEmpty()){
            return Optional.empty();
        }

        // Score based on the proportion of required skills matched.
        double skills = clamp((double) f.matchedSkills().size() / f.requiredSkillCount());

        // Full score if the minimum experience is met; otherwise, partial score
        double experience = f.minYears() <= 0 ? 1.0 : clamp((double) f.candidateYears() / f.minYears());

        double availability = switch (f.availability()){
            case IMMEDIATE -> 1.0;
            case TWO_WEEKS -> 0.5;
            case NOT_LOOKING -> 0.0;
        };

        // Score based on matching culture keywords.
        double culture = f.cultureKeywordCount() <= 0 ? 0.0 : clamp((double) f.sharedTraits().size() / f.cultureKeywordCount());

        // Calculate the weighted total score.
        double total = SKILLS_WEIGHT * skills + EXPERIENCE_WEIGHT * experience + AVAILABILITY_WEIGHT * availability + CULTURE_WEIGHT * culture;

        return Optional.of(new Score(
                round(total, 100.0),
                round(skills, 1000.0),
                round(experience, 1000.0),
                round(availability, 1000.0),
                round(culture, 1000.0),
                buildReason(f)
        ));
    }

    // Creates a readable explanation of the match.
    private static String buildReason(MatchFeatures f){
        List<String> parts = new ArrayList<>();

        List<String> skills = f.matchedSkills().stream().sorted().toList();
        parts.add(skills.size() + "/" + f.requiredSkillCount() + " required skills (" + String.join(", ", skills) + ")");

        String experience = f.candidateYears() + " yrs vs " + f.minYears() + " required";
        parts.add(f.candidateYears() >= f.minYears() ? experience : experience + " (below minimum)");

        parts.add(switch (f.availability()){
            case IMMEDIATE -> "available immediately";
            case TWO_WEEKS -> "available in 2 weeks";
            case NOT_LOOKING -> "not currently looking";
        });

        if(!f.sharedTraits().isEmpty()){
            parts.add("culture match: " + String.join(", ", f.sharedTraits().stream().sorted().toList()));
        }

        return String.join("; ", parts);
    }

    // Keeps a value between 0 and 1.
    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    // Rounds a value to the required precision.
    private static double round(double value, double factor) {
        return Math.round(value * factor) / factor;
    }
}
