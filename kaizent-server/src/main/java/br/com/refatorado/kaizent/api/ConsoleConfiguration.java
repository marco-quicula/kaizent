package br.com.refatorado.kaizent.api;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the console (the {@code kaizent-console} jar, under {@code META-INF/resources}) at {@code
 * /}. Paths that match no file fall back to {@code index.html}, so that client-side routes such as
 * {@code /runs} work on reload. API and Actuator paths never fall back.
 */
@Configuration(proxyBeanMethods = false)
class ConsoleConfiguration implements WebMvcConfigurer {

  private static final String CONSOLE_LOCATION = "classpath:/META-INF/resources/";
  private static final String STATIC_LOCATION = "classpath:/static/";
  private static final Resource INDEX = new ClassPathResource("META-INF/resources/index.html");

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations(CONSOLE_LOCATION, STATIC_LOCATION)
        .resourceChain(true)
        .addResolver(new SpaFallbackResolver());
  }

  static final class SpaFallbackResolver extends PathResourceResolver {

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
      Resource resource = super.getResource(resourcePath, location);
      if (resource != null || isServerPath(resourcePath) || !INDEX.exists()) {
        return resource;
      }
      return INDEX;
    }

    private static boolean isServerPath(String path) {
      return path.startsWith("api/") || path.startsWith("actuator/") || path.equals("api");
    }
  }
}
