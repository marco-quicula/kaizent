package br.com.refatorado.kaizent.llm.adapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** {@link Retrying} for unit tests: no real waits, and the waits are recorded. */
public final class TestRetrying {

  private TestRetrying() {}

  /** One attempt only. */
  public static Retrying none() {
    return new Retrying(1, Duration.ZERO, 1, Duration.ofMinutes(1), duration -> {});
  }

  /** Three attempts, 2 s backoff doubling, at most 60 s; waits go to {@code waits}. */
  public static Retrying recording(List<Duration> waits) {
    return new Retrying(3, Duration.ofSeconds(2), 2, Duration.ofSeconds(60), waits::add);
  }

  public static List<Duration> waits() {
    return new ArrayList<>();
  }
}
