package software.coley.versionpatcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Plugin mojo that copies and modifies dependencies that not compliant with the specified
 * target language level.
 *
 * @author Matt Coley
 */
@Mojo(name = "patch-dependencies", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class DependencyPatcherMojo extends PatcherMojo {
	@Parameter(property = "artifacts")
	public List<String> artifacts;
	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	private ArtifactRepository localRepository;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (artifacts == null)
			return;
		if (project == null)
			throw new MojoFailureException("Failed to get project");
		// Compute internal version value
		classVersion = 44 + targetVersion;
		// Start
		patchCompiled();
		patchDependencies();
	}

	private void patchCompiled() {
		project.getBuild().getOutputDirectory();
	}

	private void patchDependencies() throws MojoFailureException, MojoExecutionException {
		logger.info("Attempting to patch " + artifacts.size() + " dependencies to target version " + targetVersion);
		List<Dependency> dependencies = project.getModel().getDependencies();
		for (Dependency dependency : dependencies) {
			String id = dependency.getGroupId() + ":" + dependency.getArtifactId();
			if (artifacts.contains(id)) {
				artifacts.remove(id);
				logger.info("Found dependency to patch: " + dependency.toString());
				if (dependency.getScope().equals("compile"))
					logger.warn(" - This dependency should be marked as 'provided' since it will be bundled as a patched class!");
				patchDependency(dependency);
			}
		}
		if (!artifacts.isEmpty())
			throw new MojoFailureException("Failed to find dependencies: " + String.join(", ", artifacts));
	}

	/**
	 * Patches all classes in the given dependency.
	 *
	 * @param dependency
	 * 		Dependency to read from.
	 *
	 * @throws MojoExecutionException
	 * 		When anything goes wrong. See the associated cause exception.
	 */
	private void patchDependency(Dependency dependency) throws MojoExecutionException {
		String path = localRepository.getBasedir() + '/'
				+ dependency.getGroupId().replace('.', '/') + '/'
				+ dependency.getArtifactId() + '/'
				+ dependency.getVersion() + '/'
				+ dependency.getArtifactId() + '-' + dependency.getVersion() + ".jar";
		path = path.replace('/', File.separatorChar);
		File input = new File(path);
		if (!input.exists())
			throw new MojoExecutionException("File not found: " + path);
		// Patch the classes by copying modified classes
		try (ZipFile zipFile = new ZipFile(input)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory())
					continue;
				if (entry.getName().endsWith(".class")) {
					byte[] in = IOUtils.toByteArray(zipFile.getInputStream(entry));
					handleClass(in);
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("File not parsable: " + path, e);
		}
	}

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
