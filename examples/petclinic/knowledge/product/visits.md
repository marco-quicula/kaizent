# Visits

A visit records that a pet will come to the clinic, with a date and a description.

## Register a visit

1. Open the owner's page (**Find Owners**, then the owner).
2. Next to the pet, click **Add Visit**.
3. Check the **Date** and type a **Description** (required, up to 255 characters).
4. Click **Add Visit**. The owner's page opens with the message "Your visit has been
   booked", and the visit appears under the pet.

## New visits are dated in the future

When you add a new visit:

- The visit form opens with **tomorrow's date** already filled in. You can change it.
- The date **must be after today**. A date of today or in the past is rejected with the
  message "Visit date must be in the future". This is by design: the form books upcoming
  visits, so a visit that already happened cannot be added with its real date.
- If you clear the **Date** field, the visit is saved without a date.

Older visits can still appear on an owner's page, for example in the sample data; the rule
only applies when a new visit is added.

## What you cannot do with visits

- **Cancel, delete or reschedule a visit: not supported.** Once booked, a visit cannot be
  changed or removed in the application. Support guidance for this demo: book a new visit
  with the right date and mention the change in its description. The original visit stays
  on the owner's page as it was.
- Book a time of day: visits have a date only.
