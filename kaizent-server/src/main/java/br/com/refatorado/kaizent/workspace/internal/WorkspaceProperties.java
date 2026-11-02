package br.com.refatorado.kaizent.workspace.internal;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code kaizent.workspace.*}.
 *
 * @param path the workspace folder; relative paths are resolved against the working directory
 */
@ConfigurationProperties("kaizent.workspace")
record WorkspaceProperties(@DefaultValue("examples/petclinic") Path path) {}
