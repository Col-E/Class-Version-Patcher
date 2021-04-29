package software.coley.versionpatcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Base for other mojos.
 *
 * @author Matt Coley
 */
@Mojo(name = "ClassVersionPatcher", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public abstract class PatcherMojo extends AbstractMojo {
	@Component
	protected Logger logger;
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	@Parameter
	protected int targetVersion;
	protected int classVersion;

	/**
	 * @param bytecode
	 * 		Class to write to the project's build output directory.
	 *
	 * @throws IOException
	 * 		When writing to the output dir fails.
	 */
	private void handleClass(byte[] bytecode) throws IOException {
		ClassReader cr = new ClassReader(bytecode);
		// Ensure directories exist to write to
		File outputRoot = new File(project.getBuild().getOutputDirectory());
		File dest = new File(outputRoot, cr.getClassName() + ".class");
		if (dest.exists())
			return;
		FileUtils.forceMkdirParent(dest);
		// Rewrite the class with the target version
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = new VersionPatcher(cw, targetVersion);
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		IOUtils.write(cw.toByteArray(), new FileOutputStream(dest));
	}
}
