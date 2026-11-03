# Running PetClinic locally

## Requirements

- Java 17 or newer.
- Git, to get the code. Maven and Gradle are not needed: the repository includes their
  wrappers.
- Docker with Docker Compose, only to run PetClinic with MySQL or PostgreSQL.

## Get the code

```bash
git clone https://github.com/spring-projects/spring-petclinic.git
cd spring-petclinic
```

This knowledge base describes commit `500158f732419217507c7656904b8e6aa1bcc0d6`; check it
out with `git checkout 500158f732419217507c7656904b8e6aa1bcc0d6` to get exactly that
version. **Run all the commands below, and in [databases.md](databases.md), in the
`spring-petclinic` folder.**

## Start the application

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080/>. On Windows, use `mvnw.cmd spring-boot:run`.

By default, PetClinic uses an **in-memory H2 database** that is created and filled with
sample data at every start. Changes are lost when the application stops.

To keep data between restarts, use MySQL or PostgreSQL; see [databases.md](databases.md).

## Common problems

- **Port 8080 is already in use:** stop the other application, or start PetClinic on
  another port with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.
- **Unsupported Java version:** check `java -version`; PetClinic needs Java 17 or newer.
