# KI-001: Owner telephone accepts only 10 digits

- **Area:** Owners
- **Status:** By design (validation rule)

## Symptom

Saving an owner fails, and the form shows "Telephone must be a 10-digit number" under
**Telephone**. It happens when the number contains spaces, dashes, dots, parentheses or a
`+`, or when it has more or fewer than 10 digits, for example `(555) 123-4567`,
`555-123-4567` or `+55 47 99999-0000`.

## Cause

The **Telephone** field accepts exactly 10 digits and nothing else. There is no support for
country codes or for numbers longer than 10 digits.

## Workaround

- Type the number as 10 digits, without formatting: `(555) 123-4567` becomes `5551234567`.
- For a number with a country code, or longer than 10 digits, keep the last 10 digits in
  **Telephone** (for example, `4799990000` for `+55 47 99999-0000`) and, if the full number
  matters, write it down outside PetClinic. PetClinic has no other field for it.
