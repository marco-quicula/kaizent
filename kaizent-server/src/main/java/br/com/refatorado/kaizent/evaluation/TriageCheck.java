package br.com.refatorado.kaizent.evaluation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * The triage-shaped answer of the {@code llm-check} command. It exercises a structured call like
 * triage's (spec 0004 EV-1, EV-2), before triage itself exists (spec 0006); it is not triage's
 * output.
 *
 * @param category what kind of ticket it is
 * @param subject a short name of the product area the ticket is about
 * @param language the ticket's language, ISO 639-1
 * @param summary one sentence on what the requester needs
 * @param confidence how sure the model is of the category, from 0 to 1
 */
record TriageCheck(
    @NotNull Category category,
    @NotBlank String subject,
    @NotBlank @Pattern(regexp = "[a-z]{2}") String language,
    @NotBlank String summary,
    @DecimalMin("0") @DecimalMax("1") double confidence) {

  /** The kind of ticket. */
  enum Category {
    QUESTION,
    BUG,
    FEATURE_REQUEST,
    OTHER
  }
}
