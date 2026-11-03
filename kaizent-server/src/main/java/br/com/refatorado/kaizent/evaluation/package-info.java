/**
 * Evaluation (spec 0003): runs the workspace's evaluation cases through the pipeline without the
 * human review, checks each expected property of the outcome and writes a report. It runs only with
 * the {@code eval} profile, against real models (ADR-0020).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Evaluation")
package br.com.refatorado.kaizent.evaluation;
