package br.com.refatorado.kaizent.run.internal;

import br.com.refatorado.kaizent.run.Run;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

interface RunRepository extends ListCrudRepository<Run, UUID> {

  /**
   * Locks the oldest run waiting to be processed, skipping runs locked by another worker
   * (ADR-0023). Must be called inside a transaction.
   */
  @Query(
      """
      SELECT * FROM run
      WHERE status = 'RECEIVED'
      ORDER BY created_at, id
      LIMIT 1
      FOR UPDATE SKIP LOCKED
      """)
  Optional<Run> lockNextReceived();

  List<Run> findByProblemId(UUID problemId);
}
