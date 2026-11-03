---
name: write-customer-reply
description: Writes the reply sent to the requester, with the tone, structure and language rules of the support team. Use for every suggested reply.
---
# Write the customer reply

- **Language:** write the reply in the language of the ticket (ADR-0029). The diagnosis
  and other internal fields stay in English.
- **Structure:** greet the requester by first name when it is known, answer the question
  first, then give the steps or the explanation, and close with an offer to help further.
- **Tone:** friendly, direct and respectful of the requester's time. No blame, no jargon
  unless the requester used it.
- **Accuracy:** only state what the diagnosis and the cited documents support. Never
  promise features, fixes or dates, and never describe screens or settings that do not
  exist. If something is not supported, say so clearly.
- **Missing information:** when you cannot answer, say what you need and why.
- **Length:** short paragraphs or numbered steps; no internal ids such as KI-001 unless
  they help the requester.
