package br.com.refatorado.kaizent;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.tngtech.archunit.core.importer.ImportOption;
import fixtures.modulith.violation.ViolationFixtureApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/** Verifies the Spring Modulith module boundaries (ADR-0006, spec 0001 AC-6). */
class ModularityTest {

  @Test
  void applicationModulesRespectTheirBoundaries() {
    ApplicationModules.of(KaizentApplication.class).verify();
  }

  @Test
  void detectsAccessToAnotherModulesInternals() {
    // The fixture is test code, so it must be imported explicitly.
    ApplicationModules fixture =
        ApplicationModules.of(ViolationFixtureApplication.class, (ImportOption) location -> true);

    assertThatExceptionOfType(Violations.class)
        .isThrownBy(fixture::verify)
        .withMessageContaining("OrderService")
        .withMessageContaining("StockLedger");
  }
}
