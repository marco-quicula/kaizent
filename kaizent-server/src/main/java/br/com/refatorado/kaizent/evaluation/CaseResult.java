package br.com.refatorado.kaizent.evaluation;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The outcome of one evaluation case.
 *
 * @param runId the evaluation run, if one was started
 * @param runStatus the run's final status, or why the case did not finish
 * @param results the result per property, in the order of the case file
 * @param suggestedReply the reply that was checked, if any
 */
record CaseResult(
    String ticket,
    Optional<UUID> runId,
    String runStatus,
    Duration duration,
    Map<String, CheckResult> results,
    Optional<String> suggestedReply) {

  CaseResult {
    results = new LinkedHashMap<>(results);
  }

  long count(CheckResult.Verdict verdict) {
    return results.values().stream().filter(result -> result.verdict() == verdict).count();
  }
}
