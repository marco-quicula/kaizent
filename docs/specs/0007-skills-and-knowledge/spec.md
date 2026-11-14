# Spec 0007: Skills and knowledge

- **Status:** Approved
- **Wave:** 1
- **Depends on:** 0003
- **Related ADRs:** 0010, 0011, 0021, 0027

## Goal

Give the agent its two sources of expertise: **skills** in the Agent Skills format, with
versions, and a searchable **knowledge base** of Markdown files. Both are scoped by the
role and exposed through ports, which the resolution step (0008) and the lessons (0011)
build on.

## User stories

- As a **workspace owner**, I want to write skills and knowledge as Markdown in git, so
  that they are reviewable and versioned like code.
- As a **reviewer**, I want to open any cited document from the console, so that I can
  check the agent's sources.

## Acceptance criteria

### Skills

- **AC-1:** Skills are loaded from `skills/<name>/SKILL.md`. The front matter must
  contain `name` (equal to the folder name) and `description`. Invalid skills fail
  workspace validation.
- **AC-2:** `SkillRepository.catalog(role)` returns `name` and `description` for the
  role's allowed skills only. `SkillRepository.load(name)` returns the full content and
  the list of resource files in the skill folder.
- **AC-3:** Each skill has a version: the last commit that touched its folder when the
  workspace is a git working copy (read-only, JGit), otherwise a SHA-256 of the folder's
  content. A commit does not identify what was actually read (the working copy can hold
  uncommitted changes, and new lessons have no commit), so every `load` also returns a
  `contentHash`: the SHA-256 of the content returned. The run records both (spec 0009 AC-3).

### Knowledge

- **AC-4:** At startup, every `knowledge/**/*.md` file is indexed in an in-memory Lucene
  index, chunked by Markdown heading. Each chunk keeps its path, title, heading and
  front matter tags.
- **AC-5:** `KnowledgeSource.search(query, limit, roots)` returns ranked hits (`path`,
  `heading`, `snippet`, `score`), restricted to the given roots (the role's
  `knowledgePaths`).
- **AC-6:** `KnowledgeSource.read(path, section?)` returns a document, or one section of
  it, with its version (as in AC-3). The returned text is capped at a configurable size
  (`kaizent.knowledge.max-read`, default 16 KB). When the document or section is longer,
  the result carries `truncated = true`, the number of characters returned and the total,
  and the outline of the document's section headings, so that the caller can read another
  section. A section that is itself longer than the cap is cut the same way. The result also
  carries a `contentHash`, the SHA-256 of the text returned (the section or the document,
  after the cap), besides the version of AC-3, so that a reader can tell later whether the
  document changed since it was read.
- **AC-7:** Every path is normalized and must resolve inside `knowledge/` (for
  `KnowledgeSource`) or `skills/` (for `SkillRepository`). Anything else, including
  `..` and symbolic links that point outside, is rejected.
- **AC-8:** Adding, changing or deleting a knowledge file updates the index within 5
  seconds, without a restart.
- **AC-9:** `KnowledgeWriter.write(relativePath, content)` creates a file under
  `knowledge/lessons/` only and triggers a re-index. It never overwrites an existing file
  with different content and never creates a second file for the same call: writing
  the same path with identical content again succeeds without changing anything (so that a
  caller can resume after a failure, spec 0011 AC-7), and writing it with different
  content is refused. Re-indexing can be repeated: running it again for a file that is
  already indexed leaves the index unchanged.
- **AC-10:** `GET /api/v1/knowledge/document?path=&section=` returns the Markdown content
  for the console. Paths outside `knowledge/` return 400.

## Evaluation cases

None directly. Retrieval quality is measured through the resolution evaluations (0008).

## Out of scope

- Vector search (wave 3), and knowledge from MCP servers (wave 2).
- Editing skills from the console, and skill improvement proposals (wave 2).

## Open questions

None.
