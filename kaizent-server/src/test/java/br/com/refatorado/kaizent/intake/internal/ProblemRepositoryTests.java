package br.com.refatorado.kaizent.intake.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.refatorado.kaizent.Database;
import br.com.refatorado.kaizent.KaizentIntegrationTest;
import br.com.refatorado.kaizent.intake.Problem;
import br.com.refatorado.kaizent.intake.ProblemAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/** Spec 0002 T2: the problem table and its mapping, including the array and jsonb columns. */
@KaizentIntegrationTest
class ProblemRepositoryTests {

  @Autowired private ProblemRepository problems;
  @Autowired private JdbcAggregateTemplate aggregates;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    Database.clear(jdbc);
  }

  @Test
  void storesAndReadsAProblem() {
    Problem problem = problem("PC-100");

    aggregates.insert(problem);

    Problem loaded = problems.findById(problem.id()).orElseThrow();
    assertThat(loaded).isEqualTo(problem);
    assertThat(loaded.labels()).containsExactly("owners", "forms");
    assertThat(loaded.attributes().values()).containsEntry("file", "PC-100.md");
    assertThat(problems.existsBySourceAndExternalKey("file-inbox", "PC-100")).isTrue();
    assertThat(problems.existsBySourceAndExternalKey("file-inbox", "PC-101")).isFalse();
  }

  @Test
  void rejectsASecondProblemWithTheSameSourceAndKey() {
    aggregates.insert(problem("PC-100"));

    assertThatExceptionOfType(DuplicateKeyException.class)
        .isThrownBy(() -> aggregates.insert(problem("PC-100")));
  }

  static Problem problem(String key) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    return new Problem(
        UUID.randomUUID(),
        "file-inbox",
        key,
        "Title " + key,
        "Body of " + key,
        "Ana",
        "email",
        now.minusSeconds(60),
        now,
        List.of("owners", "forms"),
        "hash-" + key,
        new ProblemAttributes(Map.of("file", key + ".md")));
  }
}
