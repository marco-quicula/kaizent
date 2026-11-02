package br.com.refatorado.kaizent.intake.internal;

import br.com.refatorado.kaizent.intake.Problem;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

interface ProblemRepository extends ListCrudRepository<Problem, UUID> {

  boolean existsBySourceAndExternalKey(String source, String externalKey);
}
