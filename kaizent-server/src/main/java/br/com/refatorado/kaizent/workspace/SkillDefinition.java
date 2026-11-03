package br.com.refatorado.kaizent.workspace;

import java.nio.file.Path;

/**
 * A skill from {@code skills/<name>/SKILL.md}, in the Agent Skills format (ADR-0010). Only the
 * front matter is read here; the instructions are read when a run uses the skill (spec 0007).
 *
 * @param file the {@code SKILL.md} file, relative to the workspace folder
 */
public record SkillDefinition(String name, String description, Path file) {}
