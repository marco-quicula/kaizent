package br.com.refatorado.kaizent.run.internal;

import br.com.refatorado.kaizent.run.Run;
import br.com.refatorado.kaizent.run.Runs;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
class RunQueries implements Runs {

  private static final Sort NEWEST_FIRST =
      Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final RunRepository runs;

  RunQueries(RunRepository runs) {
    this.runs = runs;
  }

  @Override
  public Optional<Run> findById(UUID id) {
    return runs.findById(id);
  }

  @Override
  public RunList list(int page, int size) {
    Page<Run> result = runs.findAll(PageRequest.of(page, size, NEWEST_FIRST));
    return new RunList(result.getContent(), result.getTotalElements());
  }
}
