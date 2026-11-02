package br.com.refatorado.kaizent.run;

import java.util.UUID;

/** No run has the given id. */
public class RunNotFoundException extends RuntimeException {

  public RunNotFoundException(UUID id) {
    super("Run " + id + " does not exist");
  }
}
