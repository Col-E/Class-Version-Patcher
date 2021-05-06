package software.coley.versionpatcher.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

/**
 * Base for other patching mojos.
 *
 * @author Matt Coley
 */
public abstract class AbstractPatcherMojo extends AbstractMojo {
	@Component
	protected Logger logger;
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	@Parameter
	protected int targetVersion;
	protected int classVersion;
}
