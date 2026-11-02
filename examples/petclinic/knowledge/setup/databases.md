# Databases

PetClinic supports three databases, chosen with a Spring profile:

| Database | Profile | Notes |
|---|---|---|
| H2 (in memory) | none (default) | Data is reset at every start |
| MySQL | `mysql` | Needs a running MySQL |
| PostgreSQL | `postgres` | Needs a running PostgreSQL |

## Run with PostgreSQL

1. Start PostgreSQL. The repository's `docker-compose.yml` has a `postgres` service with the
   right settings:

   ```bash
   docker compose up postgres
   ```

2. Start PetClinic with the `postgres` profile:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

The `postgres` profile connects to `jdbc:postgresql://localhost/petclinic` (port 5432) with
the user `petclinic` and the password `petclinic`. To change them, set the environment
variables `POSTGRES_URL`, `POSTGRES_USER` and `POSTGRES_PASS`. The tables and sample data
are created at startup.

MySQL works the same way, with `docker compose up mysql` and the `mysql` profile.

## Startup fails with "Connection refused"

If the application stops at startup with
`org.postgresql.util.PSQLException: Connection to localhost:5432 refused`, PetClinic could
not reach PostgreSQL:

1. Check that the database is running: `docker compose ps` should show the `postgres`
   service as running. If not, start it with `docker compose up postgres`.
2. Check that nothing else uses port 5432 and that the database listens on `localhost`.
3. If you changed the connection, check `POSTGRES_URL`, `POSTGRES_USER` and
   `POSTGRES_PASS`.
4. Check that you meant to use PostgreSQL: without a profile, PetClinic uses H2 and needs
   no database server.

"password authentication failed" means the database is running but the user or password
does not match.
