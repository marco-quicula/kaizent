---
name: diagnose-reported-error
description: Diagnoses an error or unexpected behavior reported by a PetClinic user, telling apart a by-design rule, a known issue and a real defect. Use when the requester reports that something failed or behaves strangely.
---
# Diagnose a reported error

1. Restate the symptom: the screen, the action, the value entered and the message shown.
2. Classify it, in this order:
   - **By design:** a validation rule or behavior that the product guides describe. Explain
     the rule and how to enter valid data.
   - **Known issue:** a symptom listed in `known-issues/`. Use its explanation and
     workaround, and mention its id (for example, KI-001).
   - **Possible defect:** nothing in the knowledge base explains it. Say that it needs
     investigation, and ask for the details that are missing (steps, values, screenshots).
3. Do not call a by-design rule a bug, and do not promise a fix or a date.
4. Cite the documents that support the classification.
