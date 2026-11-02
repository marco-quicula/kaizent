# Running PetClinic locally

## Requirements

- Java 17 or newer.
- Git, to clone the repository. Maven and Gradle are not needed: the repository includes
  their wrappers.

## Start the application

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080/>. On Windows, use `mvnw.cmd spring-boot:run`.

By default, PetClinic uses an **in-memory H2 database** that is created and filled with
sample data at every start. Changes are lost when the application stops. The H2 console is
at <http://localhost:8080/h2-console>.

To keep data between restarts, use MySQL or PostgreSQL; see [databases.md](databases.md).

## Common problems

- **Port 8080 is already in use:** stop the other application, or start PetClinic on
  another port with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.
- **Unsupported Java version:** check `java -version`; PetClinic needs Java 17 or newer.
