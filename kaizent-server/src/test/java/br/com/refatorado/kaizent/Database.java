package br.com.refatorado.kaizent;

import org.springframework.jdbc.core.JdbcTemplate;

/** Helpers for tests that share the database. */
public final class Database {

  private Database() {}

  /** Deletes all runs and problems. */
  public static void clear(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM run");
    jdbc.update("DELETE FROM problem");
  }
}
