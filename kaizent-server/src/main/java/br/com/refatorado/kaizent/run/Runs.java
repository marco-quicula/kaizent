package br.com.refatorado.kaizent.run;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read access to runs for other modules. */
public interface Runs {

  Optional<Run> findById(UUID id);

  /** A page of runs, newest first. */
  RunList list(int page, int size);

  /** A page of runs and the total number of runs. */
  record RunList(List<Run> items, long totalItems) {}
}
