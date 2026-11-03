package br.com.refatorado.kaizent.run;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read access to runs for other modules. */
public interface Runs {

  Optional<Run> findById(UUID id);

  /**
   * A page of runs reviewed by people, newest first. Evaluation runs ({@link ReviewMode#SKIP}) are
   * left out, whatever their status.
   */
  RunList list(int page, int size);

  /** A page of runs and the total number of runs that the list includes. */
  record RunList(List<Run> items, long totalItems) {}
}
