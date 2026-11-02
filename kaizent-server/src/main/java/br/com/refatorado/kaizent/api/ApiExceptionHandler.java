package br.com.refatorado.kaizent.api;

import br.com.refatorado.kaizent.review.UnsupportedVerdictException;
import br.com.refatorado.kaizent.run.RunNotFoundException;
import br.com.refatorado.kaizent.run.RunStateException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain errors to RFC 9457 problem details, as declared in {@code api/openapi.yaml}. */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
class ApiExceptionHandler {

  @ExceptionHandler(RunNotFoundException.class)
  ProblemDetail notFound(RunNotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "Run not found", e.getMessage());
  }

  @ExceptionHandler(RunStateException.class)
  ProblemDetail conflict(RunStateException e) {
    return problem(HttpStatus.CONFLICT, "Run is not awaiting review", e.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail concurrentChange(OptimisticLockingFailureException e) {
    return problem(
        HttpStatus.CONFLICT, "Run was changed concurrently", "Reload the run and try again.");
  }

  @ExceptionHandler(UnsupportedVerdictException.class)
  ProblemDetail unsupportedVerdict(UnsupportedVerdictException e) {
    return problem(HttpStatus.BAD_REQUEST, "Unsupported verdict", e.getMessage());
  }

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }
}
