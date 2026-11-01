package br.com.refatorado.kaizent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Kaizent application. Each top-level package below this one is a Spring Modulith application
 * module (ADR-0006, ADR-0019).
 */
@SpringBootApplication
public class KaizentApplication {

  public static void main(String[] args) {
    SpringApplication.run(KaizentApplication.class, args);
  }
}
