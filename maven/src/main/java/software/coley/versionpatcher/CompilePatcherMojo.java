package software.coley.versionpatcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Plugin mojo modifies the classes of the compile output to be compliant with the target version.
 *
 * @author Matt Coley
 */
@Mojo(name = "ClassVersionPatcher", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class CompilePatcherMojo extends PatcherMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (project == null)
			throw new MojoFailureException("Failed to get project");
		// Compute internal version value
		classVersion = 44 + targetVersion;
		// Start
		patchCompiled();
	}

	private void patchCompiled() {
		project.getBuild().getOutputDirectory();
	}
}
