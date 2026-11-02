/**
 * Intake: turns incoming tickets into canonical {@link br.com.refatorado.kaizent.intake.Problem}s
 * (ADR-0014, ADR-0028). Every new problem is announced with a {@link
 * br.com.refatorado.kaizent.intake.ProblemReceived} event.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Intake")
package br.com.refatorado.kaizent.intake;
