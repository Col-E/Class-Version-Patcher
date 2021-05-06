package software.coley.versionpatcher.maven;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import software.coley.versionpatcher.VersionPatcher;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Plugin mojo modifies the classes of the compile output to be compliant with the target version.
 *
 * @author Matt Coley
 */
@Mojo(name = "patch-compiled", defaultPhase = LifecyclePhase.COMPILE)
public class CompilePatcherMojo extends AbstractPatcherMojo {
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (project == null)
			throw new MojoFailureException("Failed to get project");
		// Compute internal version value
		classVersion = 44 + targetVersion;
		// Start
		try {
			patchCompiled();
		} catch (IOException ex) {
			logger.error("Failed to patch compiled classes due to IO error.", ex);
		}
	}

	private void patchCompiled() throws IOException {
		Path outputRoot = Paths.get(project.getBuild().getOutputDirectory());
		Files.walk(outputRoot).filter(p -> p.toString().endsWith(".class")).forEach(p -> {
			try {
				ClassReader cr = new ClassReader(Files.readAllBytes(p));
				ClassWriter cw = new ClassWriter(0);
				ClassVisitor cv = new VersionPatcher(cw, targetVersion);
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
				IOUtils.write(cw.toByteArray(), new FileOutputStream(p.toFile()));
			} catch (Exception ex) {
				logger.error("Failed to patch class '" + p.getFileName().toString() + "'", ex);
			}
		});
	}
}
