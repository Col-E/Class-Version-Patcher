package software.coley.versionpatcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Plugin mojo that adds in extra resources to the compile output.
 *
 * @author Matt Coley
 */
@Mojo(name = "patch-postprocess", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PostProcessMojo extends PatcherMojo {
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (classVersion < 9) {
				addCompatibilityClasses();
			}
		} catch (IOException ex) {
			logger.error("Failed to add compatibility classes in post-processing", ex);
		}
	}

	/**
	 * Adds compatibility classes to the project's class-path via its build output directory.
	 *
	 * @throws IOException
	 * 		When writing to the output dir fails.
	 */
	private void addCompatibilityClasses() throws IOException {
		File outputRoot = new File(project.getBuild().getOutputDirectory());
		File stringCompatClass = new File(outputRoot, StringCompatDumper.CLASS_NAME + ".class");
		FileUtils.forceMkdirParent(stringCompatClass);
		IOUtils.write(StringCompatDumper.dump(), new FileOutputStream(stringCompatClass));
	}
}
