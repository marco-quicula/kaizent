# Plan 0007: Skills and knowledge

## Ports

```java
// skills module
interface SkillRepository {
  List<SkillSummary> catalog(Role role);
  SkillContent load(String name);                 // throws if the skill is unknown
}
record SkillSummary(String name, String description, String version) {}
record SkillContent(String name, String description, String body, List<String> resources,
                    String version, String contentHash) {}
// KnowledgeDocument(path, section, content, version, contentHash, truncated, totalChars, outline)

// knowledge module
interface KnowledgeSource {
  List<KnowledgeHit> search(String query, int limit, List<String> roots);
  KnowledgeDocument read(String path, @Nullable String section);   // capped: truncated, totalChars, outline
}
interface KnowledgeWriter {
  // Idempotent: the same name and content again is a no-op; the same name with other content is refused.
  KnowledgeDocument writeLesson(String fileName, String content);
}
```

## Components

| Module | Component | Responsibility |
|---|---|---|
| `workspace` | `WorkspacePaths` | Safe resolution (`normalize`, `toRealPath`, `startsWith(root)`), shared by every module |
| `workspace` | `VersionResolver` | JGit read-only last commit per path, with a content-hash fallback |
| `skills` | `FileSkillRepository` | Parses `SKILL.md` and caches it; reloaded on file change |
| `knowledge` | `MarkdownChunker` | Splits a document into heading-based chunks (CommonMark parser) |
| `knowledge` | `LuceneKnowledgeIndex` | `ByteBuffersDirectory`, `StandardAnalyzer`, fields `path`, `title`, `heading`, `tags`, `text`; BM25; highlighter for snippets |
| `knowledge` | `KnowledgeWatcher` | `WatchService` over `knowledge/`, debounced re-indexing |
| `knowledge` | `FileKnowledgeWriter` | Creates files atomically under `knowledge/lessons/` (a temporary file, then a move that does not replace); a second write of identical content is a no-op, and the re-index it triggers is repeatable |
| `api` | `KnowledgeController` | Document read endpoint |

## Data

No database changes.

## API

`GET /knowledge/document?path=&section=` → `{ path, section?, content, version, contentHash, truncated, totalChars, outline? }`.

## Dependencies

Apache Lucene (`lucene-core`, `lucene-queryparser`, `lucene-highlighter`), Eclipse JGit
and commonmark-java. All three are compatible with Apache-2.0: Lucene is Apache-2.0,
JGit is EDL (BSD-style) and commonmark-java is BSD-2-Clause.

## Testing

| AC | Test |
|---|---|
| AC-1 to AC-3 | `FileSkillRepository` tests on fixtures, inside and outside a git repository (JGit test repository) |
| AC-4 to AC-6 | Index tests on the demo knowledge (for example, the query "telephone" ranks KI-001 first) |
| AC-7 | Path traversal and symbolic link tests |
| AC-8 | Watcher test with Awaitility |
| AC-3 | The `contentHash` of a skill and of a document changes with an uncommitted edit, while the commit version does not; a new lesson has a hash and no commit |
| AC-6 | Read tests: a long document and a long section are cut with the marker, the totals and the outline, and another section can be read |
| AC-9 | Writer tests (new file, same content again, different content, outside `lessons/`; a re-index repeated twice) |
| AC-10 | API tests |

## Risks and mitigations

- `WatchService` behaves differently across operating systems → debounce, and also
  re-index on demand after Kaizent's own writes.
- Large knowledge bases use a lot of memory → acceptable for the MVP; a disk-based index
  is an easy switch.
