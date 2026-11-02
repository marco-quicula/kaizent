package br.com.refatorado.kaizent.intake;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read access to stored problems for other modules. */
public interface Problems {

  Optional<Problem> findById(UUID id);

  List<Problem> findAllById(Collection<UUID> ids);
}
