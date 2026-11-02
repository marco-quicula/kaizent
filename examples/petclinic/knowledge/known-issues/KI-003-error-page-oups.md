# KI-003: The "Error" menu item shows an error page

- **Area:** Technical
- **Status:** By design (demonstration feature)

## Symptom

Clicking **Error** in the menu opens a page that says "Something happened..." with the
message "Expected: controller used to showcase what happens when an exception is thrown".
The application log shows a `java.lang.RuntimeException` with the same message, thrown by
`CrashController` for the URL `/oups`.

## Cause

The **Error** menu item exists on purpose: it triggers an exception to demonstrate how
PetClinic handles and displays errors. It is not a defect, and no data is affected.

## Workaround

None is needed. Use the other menu items; the application keeps working normally.
