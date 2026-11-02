package br.com.refatorado.kaizent.intake.internal;

import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.Problems;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProblemQueries implements Problems {

  private final ProblemRepository repository;

  ProblemQueries(ProblemRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Problem> findById(UUID id) {
    return repository.findById(id);
  }
}
