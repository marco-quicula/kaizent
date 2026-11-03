# KI-001: Owner telephone accepts only 10 digits

- **Area:** Owners
- **Status:** By design (validation rule)

## Symptom

Saving an owner fails, and the form shows "Telephone must be a 10-digit number" under
**Telephone**. It happens when the number contains spaces, dashes, dots, parentheses or a
`+`, or when it has more or fewer than 10 digits, for example `(202) 555-0147`,
`202-555-0147` or `+1 (202) 555-0147`.

## Cause

The **Telephone** field accepts exactly 10 digits and nothing else. It has no room for a
country code, and PetClinic has no other field for a telephone number.

## Workaround

- Type the number as its 10 digits, without formatting: `(202) 555-0147` becomes
  `2025550147`.
- If the number is written with a country code and the rest of it is a 10-digit number,
  as in `+1 (202) 555-0147`, type only those 10 digits: `2025550147`.
- Numbers that need more than 10 digits cannot be stored. Do not shorten or change a number
  to make it fit: the result would be a wrong contact. Register the owner with a 10-digit
  number where they can be reached, if they have one.
