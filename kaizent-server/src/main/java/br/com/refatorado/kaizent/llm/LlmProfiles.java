package br.com.refatorado.kaizent.llm;

import java.util.Map;

/** The configured {@link LlmProfile} of each pipeline step ({@code kaizent.llm.profiles.*}). */
public interface LlmProfiles {

  /** The adapter name of the scripted test adapter, which never calls a model (ADR-0020). */
  String FAKE_ADAPTER = "fake";

  /** The profiles by step name, for example {@code resolution}. */
  Map<String, LlmProfile> byStep();

  /** How the calls of a profile are paid for, from its adapter. */
  CostType costType(LlmProfile profile);
}
