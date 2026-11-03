package br.com.refatorado.kaizent.workspace;

import java.nio.file.Path;
import java.util.List;

/**
 * An evaluation case from {@code evals/cases/*.yaml} (spec 0003 AC-3).
 *
 * @param ticket the id of the ticket in {@code inbox/}
 * @param file the case file, relative to the workspace folder
 */
public record EvaluationCase(String ticket, Expectation expect, Path file) {

  /**
   * The expected outcome of a case.
   *
   * @param subjects the accepted subjects; {@code unknown} is allowed
   * @param problemType {@code BUG}, {@code HOW_TO} or {@code OTHER}
   * @param citesAny workspace-relative knowledge files, of which the proposal must cite at least
   *     one; empty when no citation is expected
   * @param mentionsAll groups of alternative phrases; the reply must contain one phrase of every
   *     group
   * @param mentionsNone phrases that the reply must not contain
   * @param lowConfidence whether the adjusted confidence must be below 0.5
   * @param ticketLanguage the ISO 639-1 language that triage must detect
   * @param replyLanguage the ISO 639-1 language of the suggested reply
   */
  public record Expectation(
      List<String> subjects,
      String problemType,
      List<String> citesAny,
      List<List<String>> mentionsAll,
      List<String> mentionsNone,
      boolean lowConfidence,
      String ticketLanguage,
      String replyLanguage) {

    public Expectation {
      subjects = List.copyOf(subjects);
      citesAny = List.copyOf(citesAny);
      mentionsAll = mentionsAll.stream().map(List::copyOf).toList();
      mentionsNone = List.copyOf(mentionsNone);
    }
  }
}
