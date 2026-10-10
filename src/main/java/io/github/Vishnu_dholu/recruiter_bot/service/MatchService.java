package io.github.Vishnu_dholu.recruiter_bot.service;

import io.github.Vishnu_dholu.recruiter_bot.dto.MatchResult;
import io.github.Vishnu_dholu.recruiter_bot.model.MatchFeatures;
import io.github.Vishnu_dholu.recruiter_bot.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Calculates the candidate-job matches and provides ranked results.
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository repository;
    private final MatchScorer scorer = new MatchScorer();

    public MatchService(MatchRepository repository){
        this.repository = repository;
    }

    /**
     * Recomputes every match from scratch. Runs once when the app is ready.
     * Deletes old matches and saves the new results in one transaction.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recomputeMatches(){
        long start = System.currentTimeMillis();

        /** Fetch features for all candidate-job pairs that share a required skill */
        List<MatchFeatures> pairs = repository.findAllFeatures();
        /** Remove previous results before saving updated scores */
        repository.deleteAllMatches();

        int stored = 0;
        for(MatchFeatures features : pairs){
            // Calculate the score and skip pairs that are not valid matches.
            Optional<MatchScorer.Score> score = scorer.score(features);
            if(score.isPresent()){
                repository.insertMatch(features.jobId(), features.candidateId(), score.get());
                stored++;
            }
        }

        // Log the number of matches and the time taken.
        log.info("Computed {} matches from {} candidate/job pairs in {} ms", stored, pairs.size(), System.currentTimeMillis() - start);
    }

    /**
     * Returns ranked candidates for a job.
     * Returns Optional.empty() if the job does not exist.
     */
    public Optional<List<MatchResult>> matchesForJob(long jobId){
        if(!repository.jobExists(jobId)){
            return Optional.empty();
        }
        return Optional.of(repository.findMatchesForJob(jobId));
    }

    /**
     * Returns ranked jobs for a candidate.
     * Returns Optional.empty() if the candidate does not exist.
     */
    public Optional<List<MatchResult>> matchesForCandidate(long candidateId){
        if(!repository.candidateExists(candidateId)){
            return Optional.empty();
        }

        return Optional.of(repository.findMatchesForCandidate(candidateId));
    }
}
