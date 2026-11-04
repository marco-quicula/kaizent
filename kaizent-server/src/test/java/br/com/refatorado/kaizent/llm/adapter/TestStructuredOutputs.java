package br.com.refatorado.kaizent.llm.adapter;

import jakarta.validation.Validation;
import tools.jackson.databind.json.JsonMapper;

/** A {@link StructuredOutputs} for unit tests, without a Spring context. */
public final class TestStructuredOutputs {

  private TestStructuredOutputs() {}

  public static StructuredOutputs create() {
    return new StructuredOutputs(
        JsonMapper.shared(), Validation.buildDefaultValidatorFactory().getValidator());
  }
}
