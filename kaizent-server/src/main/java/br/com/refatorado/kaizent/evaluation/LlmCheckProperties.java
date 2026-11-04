package br.com.refatorado.kaizent.evaluation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code kaizent.llm-check.*}: one triage-shaped model call on one ticket (spec 0004 T13).
 *
 * @param ticket the ticket key, for example {@code PC-002}, read from the workspace's inbox
 * @param step the pipeline step whose profile (adapter, model, preset) serves the call
 * @param exitWhenDone whether to stop the application after the check
 */
@ConfigurationProperties("kaizent.llm-check")
record LlmCheckProperties(
    String ticket,
    @DefaultValue("resolution") String step,
    @DefaultValue("true") boolean exitWhenDone) {}
