package br.com.refatorado.kaizent.resolution;

import br.com.refatorado.kaizent.llm.Usage;

/**
 * The proposed resolution of a problem. Spec 0008 adds citations, skills and the adjusted
 * confidence.
 *
 * @param diagnosis what the problem is, for the reviewer
 * @param suggestedReply the reply to send to the requester
 * @param rawConfidence the model's own confidence, from 0 to 1
 * @param usage the usage of the model calls that produced the proposal (spec 0004 AC-5); absent in
 *     proposals stored before it was recorded
 */
public record Proposal(String diagnosis, String suggestedReply, double rawConfidence, Usage usage) {

  /** A proposal without recorded usage. */
  public Proposal(String diagnosis, String suggestedReply, double rawConfidence) {
    this(diagnosis, suggestedReply, rawConfidence, null);
  }
}
