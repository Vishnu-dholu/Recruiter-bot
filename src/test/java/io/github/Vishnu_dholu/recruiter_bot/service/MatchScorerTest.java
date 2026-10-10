package io.github.Vishnu_dholu.recruiter_bot.service;


import io.github.Vishnu_dholu.recruiter_bot.model.MatchFeatures;
import io.github.Vishnu_dholu.recruiter_bot.model.MatchFeatures.Availability;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchScorerTest {
    private final MatchScorer scorer = new MatchScorer();

    private static MatchFeatures features(List<String> matched, int required, int years, int minYears,
                                          Availability availability, List<String> shared, int keywords) {
        return new MatchFeatures(1L, 1L, matched, required, years, minYears, availability, shared, keywords);
    }

    private MatchScorer.Score scoreOf(MatchFeatures f) {
        Optional<MatchScorer.Score> result = scorer.score(f);
        assertTrue(result.isPresent(), "expected a match");
        return result.get();
    }

    @Test
    void oliviaForIncidentCommander_scoresNinety() {
        // 3/3 skills, 13 yrs vs 4, two weeks, 1 of 2 culture keywords ("decisive")
        MatchScorer.Score s = scoreOf(features(
                List.of("crisis-management", "negotiation", "strategy"), 3, 13, 4,
                Availability.TWO_WEEKS, List.of("decisive"), 2));
        assertEquals(90.00, s.total(), 0.001);   // 60 + 20 + 5 + 5
        assertEquals(1.0, s.skillScore(), 0.001);
        assertEquals(0.5, s.availabilityScore(), 0.001);
        assertEquals(0.5, s.cultureScore(), 0.001);
        assertEquals("3/3 required skills (crisis-management, negotiation, strategy); "
                + "13 yrs vs 4 required; available in 2 weeks; culture match: decisive", s.reason());
    }

    @Test
    void katnissForIncidentCommander_partialSkills() {
        MatchScorer.Score s = scoreOf(features(
                List.of("crisis-management", "strategy"), 3, 5, 4,
                Availability.IMMEDIATE, List.of("decisive"), 2));
        assertEquals(75.00, s.total(), 0.001);   // 40 + 20 + 10 + 5
        assertEquals(0.667, s.skillScore(), 0.001);
    }

    @Test
    void sherlockForBackendDetective_scoresNinetyFive() {
        MatchScorer.Score s = scoreOf(features(
                List.of("deduction", "pattern-recognition", "forensics"), 3, 8, 3,
                Availability.IMMEDIATE, List.of("analytical"), 2));
        assertEquals(95.00, s.total(), 0.001);
    }

    @Test
    void noSkillOverlap_isNotAMatch() {
        assertFalse(scorer.score(features(
                List.of(), 3, 20, 1, Availability.IMMEDIATE, List.of("analytical", "autonomous"), 2)).isPresent());
    }

    @Test
    void experienceBelowMinimum_getsProportionalCreditAndIsFlagged() {
        // 2 yrs against a minimum of 4: not in the seed data, so tested by hand
        MatchScorer.Score s = scoreOf(features(
                List.of("a", "b"), 2, 2, 4, Availability.IMMEDIATE, List.of(), 0));
        assertEquals(0.5, s.experienceScore(), 0.001);
        assertEquals(80.00, s.total(), 0.001);   // 60 + 10 + 10 + 0
        assertTrue(s.reason().contains("2 yrs vs 4 required (below minimum)"));
    }

    @Test
    void overQualified_isNotPenalised() {
        MatchScorer.Score s = scoreOf(features(
                List.of("rapid-prototyping", "systems-design"), 2, 20, 2,
                Availability.IMMEDIATE, List.of(), 2));
        assertEquals(1.0, s.experienceScore(), 0.001);
        assertFalse(s.reason().contains("below minimum"));
    }

    @Test
    void notLooking_getsZeroAvailabilityAndIsFlagged() {
        MatchScorer.Score s = scoreOf(features(
                List.of("leadership", "stakeholder-management"), 3, 18, 5,
                Availability.NOT_LOOKING, List.of(), 2));
        assertEquals(0.0, s.availabilityScore(), 0.001);
        assertEquals(60.00, s.total(), 0.001);   // 40 + 20 + 0 + 0
        assertTrue(s.reason().contains("not currently looking"));
    }

    @Test
    void perfectCandidate_scoresExactlyOneHundred() {
        // Proves the four weights add up to 100
        MatchScorer.Score s = scoreOf(features(
                List.of("a", "b"), 2, 10, 2, Availability.IMMEDIATE, List.of("x", "y"), 2));
        assertEquals(100.00, s.total(), 0.001);
    }

    @Test
    void zeroMinimumExperience_doesNotDivideByZero() {
        MatchScorer.Score s = scoreOf(features(
                List.of("a"), 1, 0, 0, Availability.IMMEDIATE, List.of(), 0));
        assertEquals(1.0, s.experienceScore(), 0.001);
    }

    @Test
    void jobWithNoCultureKeywords_givesZeroCultureNotNaN() {
        MatchScorer.Score s = scoreOf(features(
                List.of("a"), 1, 5, 1, Availability.IMMEDIATE, List.of(), 0));
        assertEquals(0.0, s.cultureScore(), 0.001);
    }
}
