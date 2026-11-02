# Visits

A visit records that a pet will come to the clinic, with a date and a description.

## Register a visit

1. Open the owner's page (**Find Owners**, then the owner).
2. Next to the pet, click **Add Visit**.
3. Check the **Date** and type a **Description** (required, up to 255 characters).
4. Click **Add Visit**. The owner's page opens with the message "Your visit has been
   booked", and the visit appears under the pet.

## Visit dates are in the future

Visits are booked ahead of time:

- The visit form opens with **tomorrow's date** already filled in. You can change it.
- The date **must be after today**. A visit dated today or in the past is rejected with the
  message "Visit date must be in the future". This is by design: PetClinic records upcoming
  visits, not past ones.

## What you cannot do with visits

- **Cancel, delete or reschedule a visit: not supported.** Once booked, a visit cannot be
  changed or removed in the application. Clinics usually add a new visit with the right date
  and note the change in its description.
- Book a time of day: visits have a date only.
- Send reminders to owners: PetClinic sends no notifications.
