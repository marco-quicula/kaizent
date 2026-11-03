/**
 * The workspace: the folder with the manifest, roles, skills, knowledge, inbox, outbox and
 * evaluation cases (ADR-0027). {@link br.com.refatorado.kaizent.workspace.WorkspaceLoader} reads it
 * and {@link br.com.refatorado.kaizent.workspace.WorkspaceValidator} checks it.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Workspace")
package br.com.refatorado.kaizent.workspace;
