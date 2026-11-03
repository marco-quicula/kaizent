package br.com.refatorado.kaizent.intake;

import java.util.Optional;

/**
 * Stores tickets as problems for evaluation runs (spec 0003). Unlike normal intake, it publishes no
 * {@link ProblemReceived}, so no run for human review is created; the caller starts the evaluation
 * run itself.
 */
public interface EvaluationProblems {

  /**
   * Reads the ticket from the ticket sources and stores it as a new problem.
   *
   * @param source the source to store, unique per evaluation session, for example {@code
   *     evaluation-20261103T150000Z}; a ticket can be stored once per source
   * @param ticketKey the ticket's external key, for example {@code PC-001}
   * @return the new problem, or empty if no ticket source has that ticket
   */
  Optional<Problem> store(String source, String ticketKey);
}
