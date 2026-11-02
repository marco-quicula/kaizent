package br.com.refatorado.kaizent.run;

import java.util.UUID;

/** A transition that the run's current state does not allow (ADR-0023). */
public class RunStateException extends IllegalStateException {

  private final RunStatus status;

  public RunStateException(UUID id, RunStatus status, RunStatus expected) {
    super("Run " + id + " is " + status + ", expected " + expected);
    this.status = status;
  }

  public RunStatus status() {
    return status;
  }
}
