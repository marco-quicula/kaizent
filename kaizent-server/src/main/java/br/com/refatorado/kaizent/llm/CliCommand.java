package br.com.refatorado.kaizent.llm;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One CLI call (ADR-0030, spec 0004 AC-6). Build it with {@link #builder(String, Duration)}.
 *
 * @param image the tool's image, which runs as a non-root user
 * @param args the command and its arguments, run in the image without a shell
 * @param workspace the workspace folder, needed only when paths are granted
 * @param grantedPaths workspace-relative folders or files, each mounted read-only at the same
 *     relative path under {@link CliExecutor#WORKSPACE}; nothing else of the workspace is visible
 * @param scratchFiles files written to {@link CliExecutor#SCRATCH} before the call, by file name
 * @param outputFiles files of {@link CliExecutor#SCRATCH} returned after the call, by file name
 * @param stdin the text written to the tool's standard input, which is closed afterwards; or null
 * @param env the names of the environment variables passed to the container; their values come from
 *     Kaizent's configuration ({@code .env} or the environment), and no other variable is passed
 * @param credentials the tool's login volume and where the tool keeps its login; or null
 * @param tmpfs absolute container paths, besides {@code /tmp}, that get a temporary file system
 * @param timeout the maximum duration; the container is stopped when it is reached
 */
public record CliCommand(
    String image,
    List<String> args,
    Path workspace,
    List<Path> grantedPaths,
    Map<String, String> scratchFiles,
    List<String> outputFiles,
    String stdin,
    List<String> env,
    Credentials credentials,
    List<String> tmpfs,
    Duration timeout) {

  public CliCommand {
    Objects.requireNonNull(image, "image");
    Objects.requireNonNull(timeout, "timeout");
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("A CLI call needs a positive timeout");
    }
    args = List.copyOf(args);
    grantedPaths = List.copyOf(grantedPaths);
    scratchFiles = Map.copyOf(scratchFiles);
    outputFiles = List.copyOf(outputFiles);
    env = List.copyOf(env);
    tmpfs = List.copyOf(tmpfs);
    if (!grantedPaths.isEmpty() && workspace == null) {
      throw new IllegalArgumentException("Granted paths need the workspace folder");
    }
  }

  /**
   * A named Docker volume that holds the tool's login, mounted at {@code path}. Kaizent never reads
   * it.
   */
  public record Credentials(String volume, String path) {
    public Credentials {
      Objects.requireNonNull(volume, "volume");
      Objects.requireNonNull(path, "path");
    }
  }

  public static Builder builder(String image, Duration timeout) {
    return new Builder(image, timeout);
  }

  /** Builds a {@link CliCommand}; everything but the image and the timeout is optional. */
  public static final class Builder {
    private final String image;
    private final Duration timeout;
    private final List<String> args = new ArrayList<>();
    private Path workspace;
    private final List<Path> grantedPaths = new ArrayList<>();
    private final Map<String, String> scratchFiles = new LinkedHashMap<>();
    private final List<String> outputFiles = new ArrayList<>();
    private String stdin;
    private final List<String> env = new ArrayList<>();
    private Credentials credentials;
    private final List<String> tmpfs = new ArrayList<>();

    private Builder(String image, Duration timeout) {
      this.image = image;
      this.timeout = timeout;
    }

    public Builder args(String... args) {
      this.args.addAll(List.of(args));
      return this;
    }

    public Builder args(List<String> args) {
      this.args.addAll(args);
      return this;
    }

    public Builder workspace(Path workspace) {
      this.workspace = workspace;
      return this;
    }

    public Builder grant(Path relativePath) {
      grantedPaths.add(relativePath);
      return this;
    }

    public Builder scratchFile(String name, String content) {
      scratchFiles.put(name, content);
      return this;
    }

    public Builder outputFile(String name) {
      outputFiles.add(name);
      return this;
    }

    public Builder stdin(String stdin) {
      this.stdin = stdin;
      return this;
    }

    public Builder env(String name) {
      env.add(name);
      return this;
    }

    public Builder credentials(String volume, String path) {
      this.credentials = new Credentials(volume, path);
      return this;
    }

    public Builder tmpfs(String path) {
      tmpfs.add(path);
      return this;
    }

    public CliCommand build() {
      return new CliCommand(
          image,
          args,
          workspace,
          grantedPaths,
          scratchFiles,
          outputFiles,
          stdin,
          env,
          credentials,
          tmpfs,
          timeout);
    }
  }
}
