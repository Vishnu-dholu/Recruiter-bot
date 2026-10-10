package io.github.Vishnu_dholu.recruiter_bot.controller;


import io.github.Vishnu_dholu.recruiter_bot.dto.MatchResult;
import io.github.Vishnu_dholu.recruiter_bot.service.MatchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService){
        this.matchService = matchService;
    }

    /** Ranked candidates for a job, best first. */
    @GetMapping("/jobs/{id}/matches")
    public List<MatchResult> matchesForJob(@PathVariable long id){
        return matchService.matchesForJob(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job " + id + " not found"));
    }

    /** Ranked jobs for a candidate, best first. */
    @GetMapping("/candidates/{id}/matches")
    public List<MatchResult> matchesForCandidate(@PathVariable long id){
        return matchService.matchesForCandidate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate " + id + " not found"));
    }
}
