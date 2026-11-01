# Plan 0005: Problem intake and canonical model

## Ports and components

```java
// intake module (public API)
interface TicketSource {
  String id();                        // for example "file-inbox"
  List<IncomingTicket> poll();        // everything currently available; dedup happens in the service
}
sealed interface IncomingTicket permits ParsedTicket, UnreadableTicket {}
record ParsedTicket(String externalKey, String title, String body, String requester,
                    String channel, Instant createdAt, List<String> labels,
                    String contentHash, Map<String, Object> attributes) implements IncomingTicket {}
record UnreadableTicket(String reference, String contentHash, String reason) implements IncomingTicket {}
```

| Component | Responsibility |
|---|---|
| `FileInboxTicketSource` | Lists files, enforces the size limit, decodes strict UTF-8, splits the front matter (SnakeYAML in safe mode), applies defaults, computes SHA-256 |
| `IntakeService` | For each poll result: dedupe, detect changes and conflicts, record issues, create `Problem` and `Run` in one transaction; sorts by `createdAt` |
| `IntakeIssueRepository` | Persists issues, unique on `(source, reference, content_hash, message_code)` |

## Data

`V4__intake_issue.sql`: `intake_issue(id uuid, source text, reference text,
content_hash text, severity text, code text, message text, seen_at timestamptz, unique
(source, reference, content_hash, code))`.

## API

`GET /intake-issues?severity=&page=&size=` → `IntakeIssue { id, source, reference,
severity, code, message, seenAt }`.

## Configuration

- `kaizent.intake.max-file-size: 256KB`
- `kaizent.intake.poll-interval: 10s`

## Testing

| AC | Test |
|---|---|
| AC-1 to AC-3, AC-6 | `FileInboxTicketSource` tests with fixture folders |
| AC-4, AC-5, AC-7 | `IntakeService` integration tests over repeated polls |
| AC-8 | API test |
| AC-9 | ArchUnit-style test: `intake`'s public API does not use `java.nio.file` types |

## Risks and mitigations

- Large inboxes are slow to scan → acceptable for the MVP; caching by modification time
  is a later optimization.
