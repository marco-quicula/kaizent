# Spec 0005: Problem intake and canonical model

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0002
- **Related ADRs:** 0014, 0027, 0028

## Goal

Turn the skeleton's minimal inbox reader into a robust intake: full ticket format
support, validation, deduplication and visible intake issues, all mapped to the
canonical `Problem` model that future connectors (Movidesk, Slack) will also produce.

## User stories

- As a **reviewer**, I want broken ticket files to show up as visible errors, so that
  nothing is silently ignored.
- As a **connector author** (future), I want a clear canonical model and port, so that a
  new channel only needs a mapping.

## Acceptance criteria

- **AC-1:** All the front matter fields of ADR-0028 are parsed, and the defaults apply
  when a field is missing (`id` from the file name, `title` from the first non-empty
  line, `createdAt` from the file's modification time).
- **AC-2:** Only `*.md` files directly in `inbox/` are read. Hidden files,
  subdirectories and other extensions are ignored.
- **AC-3:** A file that is not valid UTF-8, has invalid YAML front matter, has an empty
  body or exceeds `kaizent.intake.max-file-size` (default 256 KB) produces an
  `IntakeIssue` with severity `ERROR`, the file name and the reason. It creates no run.
- **AC-4:** An `IntakeIssue` is recorded once per file and content hash, not on every
  poll.
- **AC-5:** A known `(source, externalKey)` with a different `contentHash` produces one
  `WARNING` ("ticket changed after intake; changes ignored in wave 1") and no new run.
- **AC-6:** Two files declaring the same `id` produce an `ERROR` for the second one,
  naming the first file.
- **AC-7:** New tickets are processed in `createdAt` order.
- **AC-8:** `GET /api/v1/intake-issues` lists the issues, newest first.
- **AC-9:** The `TicketSource` port and the `Problem` model contain nothing specific to
  files. File details go in `attributes` (for example, `attributes.file`).

## Evaluation cases

None.

## Out of scope

- Updates to existing tickets, and replies threaded back into a ticket (later waves).
- Attachments.
- Personal data redaction (open decision, required before the first real connector).
- The console page for intake issues (spec 0010).

## Open questions

None.
