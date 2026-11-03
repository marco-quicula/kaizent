package br.com.refatorado.kaizent.evaluation;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code kaizent.eval.*}. The cases are read from the workspace's {@code evals/cases/}.
 *
 * @param outputPath the folder of the reports; relative paths are resolved against the working
 *     directory
 * @param caseTimeout how long to wait for one case's run before reporting it as timed out
 * @param caseDelay how long to wait after a case before starting the next one, whatever the
 *     adapter; zero (the default) starts the next case at once
 * @param pollInterval how often to check whether a run has finished
 * @param exitWhenDone whether to stop the application after the report is written
 */
@ConfigurationProperties("kaizent.eval")
record EvalProperties(
    @DefaultValue("target/eval") Path outputPath,
    @DefaultValue("10m") Duration caseTimeout,
    @DefaultValue("0s") Duration caseDelay,
    @DefaultValue("1s") Duration pollInterval,
    @DefaultValue("true") boolean exitWhenDone) {}
