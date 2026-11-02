# KI-002: Pet birth date in the future shows "invalid date"

- **Area:** Pets
- **Status:** By design (validation rule); the message is unclear

## Symptom

Adding or editing a pet with a birth date after today fails, and the form shows "invalid
date" under **Birth Date**, even though the date is written correctly.

## Cause

A pet's birth date cannot be in the future. The rule is intended; the message only says
"invalid date" and does not say why.

## Workaround

Enter the actual birth date, today or earlier. If the exact date is unknown, use an
estimate in the past.
