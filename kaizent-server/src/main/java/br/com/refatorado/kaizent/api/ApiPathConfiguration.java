package br.com.refatorado.kaizent.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the API controllers under {@value #BASE_PATH}, the server URL declared in {@code
 * api/openapi.yaml}. The generated interfaces carry the paths relative to it.
 */
@Configuration(proxyBeanMethods = false)
class ApiPathConfiguration implements WebMvcConfigurer {

  static final String BASE_PATH = "/api/v1";

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        BASE_PATH, HandlerTypePredicate.forBasePackageClass(ApiPathConfiguration.class));
  }
}
