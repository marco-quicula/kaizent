package br.com.refatorado.kaizent.run;

import java.time.Instant;

/**
 * A reviewer's verdict on a run's proposal.
 *
 * @param finalReply the reply that is delivered
 * @param comment the reviewer's comment, if any
 * @param reviewer who decided; wave 1 has no authentication, so it is a fixed local name
 */
public record Verdict(
    VerdictType type, String finalReply, String comment, String reviewer, Instant reviewedAt) {}
