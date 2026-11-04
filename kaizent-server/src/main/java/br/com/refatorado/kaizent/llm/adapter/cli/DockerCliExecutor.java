package br.com.refatorado.kaizent.llm.adapter.cli;

import br.com.refatorado.kaizent.llm.CliCommand;
import br.com.refatorado.kaizent.llm.CliException;
import br.com.refatorado.kaizent.llm.CliException.Kind;
import br.com.refatorado.kaizent.llm.CliExecutor;
import br.com.refatorado.kaizent.llm.CliResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs each CLI call in a new container with {@code docker run --rm} (ADR-0030, spec 0004 AC-6).
 * The container gets a read-only root file system, a non-root user, CPU, memory and process limits,
 * no Linux capabilities, only the granted workspace paths (read-only), a writable scratch folder
 * that is deleted afterwards, the tool's credential volume, and only the declared environment
 * variables. A timeout stops the container. Kaizent calls the {@code docker} command line; it needs
 * no Docker library, and it never mounts the Docker socket.
 */
public final class DockerCliExecutor implements CliExecutor {

  private static final Logger log = LoggerFactory.getLogger(DockerCliExecutor.class);

  private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
  private static final Pattern VOLUME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");
  private static final Pattern FILE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");
  private static final Pattern CONTAINER_PATH = Pattern.compile("/[A-Za-z0-9_./-]*");
  private static final String EVALS = "evals";
  private static final int DOCKER_ERROR = 125;
  private static final String REDACTED = "[REDACTED]";
  private static final Pattern USER = Pattern.compile("([0-9]+):([0-9]+)");
  private static final int STDERR_EXCERPT = 1000;
  private static final Duration KILL_TIMEOUT = Duration.ofSeconds(20);

  /**
   * The container settings: {@code kaizent.cli.*}.
   *
   * @param docker the {@code docker} command
   * @param maxOutputBytes the cap of stdout, stderr and each output file
   * @param user the non-root user and group the tool runs as, {@code uid:gid}
   * @param cpus the CPU limit, for example {@code 2}
   * @param memory the memory limit, for example {@code 2g}
   * @param pidsLimit the maximum number of processes in the container
   */
  public record Settings(
      String docker, long maxOutputBytes, String user, String cpus, String memory, int pidsLimit) {

    public Settings {
      Objects.requireNonNull(docker, "docker");
      Matcher ids = user == null ? null : USER.matcher(user);
      // Numeric only: Docker also takes names, and "root" or "00" would run the tool as root.
      if (ids == null
          || !ids.matches()
          || new java.math.BigInteger(ids.group(1)).signum() == 0
          || new java.math.BigInteger(ids.group(2)).signum() == 0) {
        throw new IllegalArgumentException(
            "kaizent.cli.user must be a numeric non-root uid:gid, for example 10001:10001");
      }
    }
  }

  private final Settings settings;
  private final Function<String, String> envValues;

  /**
   * @param envValues the value of an environment variable that a command declares, from Kaizent's
   *     configuration; null when it is not set
   */
  public DockerCliExecutor(Settings settings, Function<String, String> envValues) {
    this.settings = settings;
    this.envValues = envValues;
  }

  @Override
  public CliResult execute(CliCommand command) {
    String container = "kaizent-cli-" + UUID.randomUUID();
    Map<String, String> env = envValues(command);
    Path scratch = createScratch(command);
    long start = System.nanoTime();
    try {
      List<String> line = commandLine(command, container, scratch);
      log.debug("Running a CLI call in container {}: {}", container, line);
      Process process = start(line, env);
      Capture stdout = Capture.start(process.getInputStream(), settings.maxOutputBytes());
      Capture stderr = Capture.start(process.getErrorStream(), settings.maxOutputBytes());
      boolean finished = false;
      try {
        writeStdin(process, command.stdin());
        finished = waitFor(process, command.timeout());
      } finally {
        if (!finished) {
          // Timeout, interruption or any other failure: the container belongs to the Docker
          // daemon and keeps running unless it is stopped, even when its client is gone.
          kill(container);
          process.destroyForcibly();
        }
      }
      if (!finished) {
        throw new CliException(
            Kind.TIMEOUT,
            null,
            "CLI call in "
                + command.image()
                + " timed out after "
                + seconds(command.timeout())
                + "; the container was stopped",
            null);
      }
      String out = stdout.text();
      String err = stderr.text();
      int exit = process.exitValue();
      if (exit == DOCKER_ERROR && isDockerError(err)) {
        throw dockerError(exit, excerpt(err, stderr.truncated(), env));
      }
      if (exit != 0) {
        String excerpt =
            err.isBlank()
                ? excerpt(out, stdout.truncated(), env)
                : excerpt(err, stderr.truncated(), env);
        throw new CliException(
            Kind.NON_ZERO_EXIT,
            exit,
            "CLI call in " + command.image() + " exited with code " + exit + ": " + excerpt,
            null);
      }
      Map<String, String> files = new LinkedHashMap<>();
      boolean truncated = stdout.truncated() || stderr.truncated();
      for (String name : command.outputFiles()) {
        Path file = scratch.resolve(name);
        if (Files.isSymbolicLink(file)) {
          // The tool could point it at a host file that was never mounted.
          throw new CliException(
              Kind.FAILED,
              null,
              "CLI output file " + name + " is a symbolic link; only regular files are read",
              null);
        }
        if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
          byte[] bytes = readCapped(file);
          truncated |= bytes.length > settings.maxOutputBytes();
          int length = (int) Math.min(bytes.length, settings.maxOutputBytes());
          files.put(name, new String(bytes, 0, length, StandardCharsets.UTF_8));
        }
      }
      return new CliResult(out, err, truncated, files, Duration.ofNanos(System.nanoTime() - start));
    } finally {
      deleteScratch(scratch);
    }
  }

  private Map<String, String> envValues(CliCommand command) {
    Map<String, String> values = new LinkedHashMap<>();
    for (String name : command.env()) {
      if (!NAME.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid environment variable name: " + name);
      }
      values.put(name, envValues.apply(name));
    }
    return values;
  }

  List<String> commandLine(CliCommand command, String container, Path scratch) {
    List<String> line = new ArrayList<>();
    line.addAll(
        List.of(
            settings.docker(),
            "run",
            "--rm",
            "--interactive",
            "--name",
            container,
            "--read-only",
            "--tmpfs",
            "/tmp",
            "--user",
            settings.user(),
            "--cap-drop",
            "ALL",
            "--security-opt",
            "no-new-privileges",
            "--pids-limit",
            Integer.toString(settings.pidsLimit()),
            "--cpus",
            settings.cpus(),
            "--memory",
            settings.memory(),
            "--workdir",
            WORKSPACE));
    for (String path : command.tmpfs()) {
      line.add("--tmpfs");
      line.add(containerPath(path));
    }
    for (Path granted : command.grantedPaths()) {
      GrantedPath mount = grantedPath(command.workspace(), granted);
      line.add("--mount");
      line.add(
          "type=bind,source="
              + mountSource(mount.source())
              + ",target="
              + mount.target()
              + ",readonly");
    }
    line.add("--mount");
    line.add("type=bind,source=" + mountSource(scratch) + ",target=" + SCRATCH);
    if (command.credentials() != null) {
      String volume = command.credentials().volume();
      if (!VOLUME.matcher(volume).matches()) {
        throw new IllegalArgumentException("Invalid credential volume name: " + volume);
      }
      line.add("--mount");
      line.add(
          "type=volume,source="
              + volume
              + ",target="
              + containerPath(command.credentials().path()));
    }
    for (String name : command.env()) {
      // The value reaches docker through its own environment, never on the command line.
      line.add("--env");
      line.add(name);
    }
    if (command.image().isBlank() || command.image().startsWith("-")) {
      throw new IllegalArgumentException("Invalid image: " + command.image());
    }
    line.add(command.image());
    line.addAll(command.args());
    return line;
  }

  private record GrantedPath(Path source, String target) {}

  /** A workspace-relative path that stays inside the workspace, symbolic links included. */
  private static GrantedPath grantedPath(Path workspace, Path relative) {
    if (relative.isAbsolute()
        || relative.getNameCount() == 0
        || relative.toString().isBlank()
        || !relative.normalize().equals(relative)
        || relative.startsWith("..")) {
      throw new IllegalArgumentException(
          "A granted path must be a normalized workspace-relative path: " + relative);
    }
    try {
      Path root = workspace.toRealPath();
      Path source = root.resolve(relative).toRealPath();
      if (!source.startsWith(root)) {
        throw new IllegalArgumentException("A granted path leaves the workspace: " + relative);
      }
      // The evaluation cases hold the expected answers: never visible to a tool (ADR-0030).
      Path evals = root.resolve(EVALS);
      Path realEvals = Files.exists(evals) ? evals.toRealPath() : evals;
      if (source.startsWith(realEvals) || realEvals.startsWith(source)) {
        throw new IllegalArgumentException(
            "A granted path cannot include the evaluation cases (" + EVALS + "/): " + relative);
      }
      StringBuilder target = new StringBuilder(WORKSPACE);
      for (Path name : relative) {
        target.append('/').append(name);
      }
      return new GrantedPath(source, target.toString());
    } catch (IOException e) {
      throw new IllegalArgumentException("A granted path does not exist: " + relative, e);
    }
  }

  private static String mountSource(Path path) {
    String source = path.toAbsolutePath().toString();
    if (source.contains(",")) {
      throw new IllegalArgumentException("A mounted path cannot contain a comma: " + source);
    }
    return source;
  }

  private static String containerPath(String path) {
    if (!CONTAINER_PATH.matcher(path).matches() || path.contains("..")) {
      throw new IllegalArgumentException("Invalid container path: " + path);
    }
    return path;
  }

  private Path createScratch(CliCommand command) {
    for (String name : command.scratchFiles().keySet()) {
      checkFileName(name);
    }
    for (String name : command.outputFiles()) {
      checkFileName(name);
    }
    try {
      Path scratch = Files.createTempDirectory("kaizent-cli-");
      // The tool runs as another user: it must be able to read the inputs and write its output.
      makeShared(scratch, "rwxrwxrwx");
      for (Map.Entry<String, String> file : command.scratchFiles().entrySet()) {
        Path path = scratch.resolve(file.getKey());
        Files.writeString(path, file.getValue());
        makeShared(path, "rw-rw-rw-");
      }
      return scratch;
    } catch (IOException e) {
      throw new CliException(Kind.FAILED, null, "Could not prepare the CLI call's files", e);
    }
  }

  private static void checkFileName(String name) {
    if (!FILE.matcher(name).matches() || name.contains("..")) {
      throw new IllegalArgumentException("Invalid scratch file name: " + name);
    }
  }

  private static void makeShared(Path path, String permissions) throws IOException {
    try {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
    } catch (UnsupportedOperationException e) {
      // Not a POSIX file system (Windows): Docker Desktop shares the folder with the container.
    }
  }

  private static void deleteScratch(Path scratch) {
    try (Stream<Path> paths = Files.walk(scratch)) {
      paths.sorted(Comparator.reverseOrder()).forEach(DockerCliExecutor::deleteQuietly);
    } catch (IOException e) {
      log.warn("Could not delete the CLI scratch folder {}: {}", scratch, e.getMessage());
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.warn("Could not delete {}: {}", path, e.getMessage());
    }
  }

  private Process start(List<String> line, Map<String, String> env) {
    ProcessBuilder builder = new ProcessBuilder(line);
    // docker needs the host environment (PATH, DOCKER_HOST...); the container gets only the
    // declared variables, which docker reads from here.
    env.forEach((name, value) -> {
      if (value == null) {
        builder.environment().remove(name);
      } else {
        builder.environment().put(name, value);
      }
    });
    try {
      return builder.start();
    } catch (IOException e) {
      throw new CliException(
          Kind.DOCKER_UNAVAILABLE,
          null,
          "Docker is not available: could not run '"
              + settings.docker()
              + "'. The CLI adapters need Docker (ADR-0030); install it or start Docker Desktop",
          e);
    }
  }

  private static void writeStdin(Process process, String stdin) {
    Thread.ofVirtual()
        .name("kaizent-cli-stdin")
        .start(() -> {
          try (OutputStream in = process.getOutputStream()) {
            if (stdin != null) {
              in.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
          } catch (IOException e) {
            // The tool exited without reading its input; its exit code tells why.
          }
        });
  }

  private static boolean waitFor(Process process, Duration timeout) {
    try {
      return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // The caller stops the container; the interruption stays visible to it.
      Thread.currentThread().interrupt();
      throw new CliException(Kind.FAILED, null, "Interrupted while waiting for a CLI call", e);
    }
  }

  /** Stops the container; also when the thread was interrupted, whose flag is kept. */
  private void kill(String container) {
    boolean interrupted = Thread.interrupted();
    try {
      Process kill =
          new ProcessBuilder(settings.docker(), "kill", container)
              .redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .start();
      if (!kill.waitFor(KILL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
        kill.destroyForcibly();
        log.warn("docker kill {} did not finish in time", container);
      }
    } catch (IOException e) {
      log.warn("Could not stop container {}: {}", container, e.getMessage());
    } catch (InterruptedException e) {
      interrupted = true;
      log.warn("Interrupted while stopping container {}", container);
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Exit code 125 with Docker's own message: the container did not start. */
  private static boolean isDockerError(String stderr) {
    return stderr.contains("docker:")
        || stderr.contains("Docker daemon")
        || stderr.contains("docker daemon")
        || stderr.contains("error during connect")
        || stderr.contains("Unable to find image");
  }

  private static CliException dockerError(int exit, String excerpt) {
    String reason =
        excerpt.contains("Cannot connect to the Docker daemon")
                || excerpt.contains("docker daemon is not running")
                || excerpt.contains("error during connect")
            ? "Docker is not running; start Docker Desktop or the Docker daemon"
            : "Docker could not run the container";
    return new CliException(Kind.DOCKER_UNAVAILABLE, exit, reason + ": " + excerpt, null);
  }

  private byte[] readCapped(Path file) {
    try (InputStream in = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
      return in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, settings.maxOutputBytes() + 1));
    } catch (IOException e) {
      throw new CliException(Kind.FAILED, null, "Could not read the CLI output file " + file, e);
    }
  }

  /**
   * The start of an error output, on one line, without the values of the passed variables. Every
   * occurrence of each value is marked on the original text, overlapping ones included, and, when
   * the output was cut at {@code max-output}, also the start of a value cut off at its end; each
   * run of marked characters then becomes one {@code [REDACTED]}. Marking before replacing means
   * that no value can hide or break another. The excerpt is shortened only afterwards.
   */
  static String excerpt(String text, boolean cut, Map<String, String> env) {
    List<String> values =
        env.values().stream()
            .filter(value -> value != null && !value.isEmpty())
            .distinct()
            .toList();
    boolean[] secret = new boolean[text.length()];
    for (String value : values) {
      for (int at = text.indexOf(value); at >= 0; at = text.indexOf(value, at + 1)) {
        java.util.Arrays.fill(secret, at, at + value.length(), true);
      }
      if (cut) {
        for (int length = Math.min(value.length() - 1, text.length()); length > 0; length--) {
          if (text.regionMatches(text.length() - length, value, 0, length)) {
            java.util.Arrays.fill(secret, text.length() - length, text.length(), true);
            break;
          }
        }
      }
    }
    StringBuilder redacted = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      if (!secret[i]) {
        redacted.append(text.charAt(i));
      } else if (i == 0 || !secret[i - 1]) {
        redacted.append(REDACTED);
      }
    }
    String result = redacted.toString().strip().replaceAll("\\s+", " ");
    return result.length() <= STDERR_EXCERPT ? result : result.substring(0, STDERR_EXCERPT) + "…";
  }

  private static String seconds(Duration duration) {
    return duration.toMillis() % 1000 == 0
        ? duration.toSeconds() + "s"
        : duration.toMillis() + "ms";
  }

  /** Reads a stream to its end, keeping at most {@code max} bytes. */
  private static final class Capture {
    private final ByteArrayOutputStream kept = new ByteArrayOutputStream();
    private final long max;
    private final Thread reader;
    private volatile boolean truncated;

    private Capture(InputStream in, long max) {
      this.max = max;
      this.reader = Thread.ofVirtual().name("kaizent-cli-output").start(() -> drain(in));
    }

    static Capture start(InputStream in, long max) {
      return new Capture(in, max);
    }

    private void drain(InputStream in) {
      byte[] buffer = new byte[8192];
      try (in) {
        int read;
        while ((read = in.read(buffer)) != -1) {
          synchronized (kept) {
            long room = max - kept.size();
            if (room > 0) {
              kept.write(buffer, 0, (int) Math.min(room, read));
            }
            if (read > room) {
              truncated = true;
            }
          }
        }
      } catch (IOException e) {
        // The process ended or was stopped.
      }
    }

    String text() {
      try {
        reader.join(Duration.ofSeconds(10));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      synchronized (kept) {
        return kept.toString(StandardCharsets.UTF_8);
      }
    }

    boolean truncated() {
      return truncated;
    }
  }
}
