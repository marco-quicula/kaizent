package br.com.refatorado.kaizent.resolution;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * What the model must answer in the resolution step (spec 0004 AC-2). Its JSON schema is generated
 * from this record, and the answer is validated against it.
 *
 * @param diagnosis what the problem is, for the reviewer, in English
 * @param suggestedReply the reply to send to the requester, in the ticket's language
 * @param rawConfidence the model's own confidence, from 0 to 1
 */
public record ResolutionOutput(
    @NotBlank String diagnosis,
    @NotBlank String suggestedReply,
    @DecimalMin("0.0") @DecimalMax("1.0") double rawConfidence) {}
