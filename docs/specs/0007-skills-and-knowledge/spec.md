# Spec 0007: Skills and knowledge

- **Status:** Draft
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
  content.

### Knowledge

- **AC-4:** At startup, every `knowledge/**/*.md` file is indexed in an in-memory Lucene
  index, chunked by Markdown heading. Each chunk keeps its path, title, heading and
  front matter tags.
- **AC-5:** `KnowledgeSource.search(query, limit, roots)` returns ranked hits (`path`,
  `heading`, `snippet`, `score`), restricted to the given roots (the role's
  `knowledgePaths`).
- **AC-6:** `KnowledgeSource.read(path, section?)` returns a document, or one section of
  it, with its version (as in AC-3).
- **AC-7:** Every path is normalized and must resolve inside `knowledge/` (for
  `KnowledgeSource`) or `skills/` (for `SkillRepository`). Anything else, including
  `..` and symbolic links that point outside, is rejected.
- **AC-8:** Adding, changing or deleting a knowledge file updates the index within 5
  seconds, without a restart.
- **AC-9:** `KnowledgeWriter.write(relativePath, content)` creates a new file under
  `knowledge/lessons/` only, refuses to overwrite an existing file, and triggers a
  re-index.
- **AC-10:** `GET /api/v1/knowledge/document?path=&section=` returns the Markdown content
  for the console. Paths outside `knowledge/` return 400.

## Evaluation cases

None directly. Retrieval quality is measured through the resolution evaluations (0008).

## Out of scope

- Vector search (wave 3), and knowledge from MCP servers (wave 2).
- Editing skills from the console, and skill improvement proposals (wave 2).

## Open questions

None.
