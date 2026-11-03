package br.com.refatorado.kaizent.workspace;

/** A subject declared in {@code kaizent-workspace.yaml}: an area that problems are about. */
public record Subject(String id, String name, String description) {}
