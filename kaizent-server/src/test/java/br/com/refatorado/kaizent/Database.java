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

  /** Turns the runs of a ticket into evaluation runs ({@code review_mode = SKIP}). */
  public static void makeEvaluationRuns(JdbcTemplate jdbc, String ticketKey) {
    jdbc.update(
        "UPDATE run SET review_mode = 'SKIP'"
            + " WHERE problem_id IN (SELECT id FROM problem WHERE external_key = ?)",
        ticketKey);
  }
}
