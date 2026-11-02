package br.com.refatorado.kaizent.api;

import br.com.refatorado.kaizent.api.generated.RunsApi;
import br.com.refatorado.kaizent.api.generated.model.Failure;
import br.com.refatorado.kaizent.api.generated.model.FailureReason;
import br.com.refatorado.kaizent.api.generated.model.Proposal;
import br.com.refatorado.kaizent.api.generated.model.RunDetail;
import br.com.refatorado.kaizent.api.generated.model.RunPage;
import br.com.refatorado.kaizent.api.generated.model.RunStatus;
import br.com.refatorado.kaizent.api.generated.model.RunSummary;
import br.com.refatorado.kaizent.api.generated.model.Verdict;
import br.com.refatorado.kaizent.api.generated.model.VerdictRequest;
import br.com.refatorado.kaizent.api.generated.model.VerdictType;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.Problems;
import br.com.refatorado.kaizent.review.ReviewService;
import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.RunNotFoundException;
import br.com.refatorado.kaizent.run.Runs;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/runs}: the run queue, the run detail and the verdict (spec 0002). */
@RestController
class RunsController implements RunsApi {

  private final Runs runs;
  private final Problems problems;
  private final ReviewService reviews;

  RunsController(Runs runs, Problems problems, ReviewService reviews) {
    this.runs = runs;
    this.problems = problems;
    this.reviews = reviews;
  }

  @Override
  public RunPage listRuns(Integer page, Integer size) {
    Runs.RunList list = runs.list(page, size);
    Map<UUID, Problem> problemsById =
        problems.findAllById(list.items().stream().map(Run::problemId).toList()).stream()
            .collect(Collectors.toMap(Problem::id, Function.identity()));
    return new RunPage(
        list.items().stream().map(run -> summary(run, problemsById.get(run.problemId()))).toList(),
        page,
        size,
        list.totalItems());
  }

  @Override
  public RunDetail getRun(UUID id) {
    return detail(runs.findById(id).orElseThrow(() -> new RunNotFoundException(id)));
  }

  @Override
  public RunDetail recordVerdict(UUID id, VerdictRequest request) {
    return detail(
        reviews.recordVerdict(
            id, br.com.refatorado.kaizent.run.VerdictType.valueOf(request.getType().name())));
  }

  private static RunSummary summary(Run run, Problem problem) {
    return new RunSummary(
        run.id(),
        problem.externalKey(),
        problem.title(),
        RunStatus.fromValue(run.status().name()),
        time(run.createdAt()));
  }

  private RunDetail detail(Run run) {
    Problem problem =
        problems
            .findById(run.problemId())
            .orElseThrow(() -> new IllegalStateException("Problem not found: " + run.problemId()));
    RunDetail detail =
        new RunDetail(
            run.id(),
            RunStatus.fromValue(run.status().name()),
            problem(problem),
            time(run.createdAt()),
            time(run.updatedAt()));
    if (run.proposal() != null) {
      detail.proposal(
          new Proposal(
              run.proposal().diagnosis(),
              run.proposal().suggestedReply(),
              run.proposal().rawConfidence()));
    }
    if (run.verdict() != null) {
      detail.verdict(
          new Verdict(
                  VerdictType.fromValue(run.verdict().type().name()),
                  run.verdict().finalReply(),
                  time(run.verdict().reviewedAt()))
              .comment(run.verdict().comment())
              .reviewer(run.verdict().reviewer()));
    }
    if (run.failureReason() != null) {
      detail.failure(
          new Failure(FailureReason.fromValue(run.failureReason().name()), run.failureMessage()));
    }
    return detail;
  }

  private static br.com.refatorado.kaizent.api.generated.model.Problem problem(Problem problem) {
    return new br.com.refatorado.kaizent.api.generated.model.Problem(
            problem.externalKey(),
            problem.source(),
            problem.title(),
            problem.body(),
            problem.labels(),
            time(problem.createdAt()),
            time(problem.receivedAt()))
        .requester(problem.requester())
        .channel(problem.channel());
  }

  private static OffsetDateTime time(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
