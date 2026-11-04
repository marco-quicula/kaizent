package br.com.refatorado.kaizent.llm.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * {@code kaizent.cli.*}: how CLI tools run in containers (ADR-0030, spec 0004 AC-6).
 *
 * @param docker the {@code docker} command
 * @param maxOutput the cap of each call's standard output, standard error and output files
 * @param user the non-root {@code uid:gid} the tools run as; the CLI images create this user
 * @param limits the container's resource limits
 */
@ConfigurationProperties("kaizent.cli")
record CliProperties(
    @DefaultValue("docker") String docker,
    @DefaultValue("2MB") DataSize maxOutput,
    @DefaultValue("10001:10001") String user,
    @DefaultValue Limits limits) {

  /**
   * @param cpus the CPU limit
   * @param memory the memory limit, in Docker's format
   * @param pids the maximum number of processes
   */
  record Limits(
      @DefaultValue("2") String cpus,
      @DefaultValue("2g") String memory,
      @DefaultValue("256") int pids) {}
}
