package br.com.refatorado.kaizent.resolution;

/**
 * The proposed resolution of a problem. Spec 0008 adds citations, skills and the adjusted
 * confidence.
 *
 * @param diagnosis what the problem is, for the reviewer
 * @param suggestedReply the reply to send to the requester
 * @param rawConfidence the model's own confidence, from 0 to 1
 */
public record Proposal(String diagnosis, String suggestedReply, double rawConfidence) {}
