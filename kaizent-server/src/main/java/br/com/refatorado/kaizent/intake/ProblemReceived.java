package br.com.refatorado.kaizent.intake;

import java.util.UUID;

/**
 * Published, inside the intake transaction, when a new problem is stored. Listeners that react
 * synchronously (such as run creation) commit or roll back together with the problem.
 */
public record ProblemReceived(UUID problemId, String source, String externalKey) {}
