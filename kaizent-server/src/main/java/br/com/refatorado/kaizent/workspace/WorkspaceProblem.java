package br.com.refatorado.kaizent.workspace;

import java.nio.file.Path;

/**
 * Something wrong in a workspace file, found while loading or validating it.
 *
 * @param file the file, relative to the workspace folder
 * @param message what is wrong, in English
 */
public record WorkspaceProblem(Path file, String message) {

  @Override
  public String toString() {
    return file.toString().replace('\\', '/') + ": " + message;
  }
}
