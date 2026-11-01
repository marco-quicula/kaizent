/**
 * The REST API (ADR-0024). Controllers implement the interfaces generated from {@code
 * api/openapi.yaml} and depend only on the other modules' public APIs.
 */
@org.springframework.modulith.ApplicationModule(displayName = "REST API")
package br.com.refatorado.kaizent.api;
