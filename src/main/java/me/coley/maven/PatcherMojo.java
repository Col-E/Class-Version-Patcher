package me.coley.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import org.codehaus.plexus.logging.Logger;
import org.objectweb.asm.RecordComponentVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Plugin mojo that does all the witch-craft.
 *
 * @author Matt Coley
 */
@Mojo(name = "ClassVersionPatcher", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PatcherMojo extends AbstractMojo {
	@Component
	private Logger logger;
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	public MavenProject project;
	@Parameter(property = "artifacts")
	public List<String> artifacts;
	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	private ArtifactRepository localRepository;
	@Parameter
	private int targetVersion;
	private int convertedVersion;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (artifacts == null)
			return;
		if (project == null)
			throw new MojoFailureException("Failed to get project");
		// Compute internal version value
		convertedVersion = 44 + targetVersion;
		// Start
		logger.info("Attempting to patch " + artifacts.size() + " dependencies to target version " + targetVersion);
		List<Dependency> dependencies = project.getModel().getDependencies();
		for (Dependency dependency : dependencies) {
			String id = dependency.getGroupId() + ":" + dependency.getArtifactId();
			if (artifacts.contains(id)) {
				artifacts.remove(id);
				logger.info("Found dependency to patch: " + dependency.toString());
				if (dependency.getScope().equals("compile"))
					logger.warn(" - This dependency should be marked as 'runtime' since it will be bundled as a patched class!");
				patch(dependency);
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
	private void patch(Dependency dependency) throws MojoExecutionException {
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
		// Add compat classes
		try {
			if (targetVersion < 9)
				addCompatibilityClasses();
		} catch (IOException e) {
			throw new MojoExecutionException("Failed adding compatibility classes to output", e);
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
		FileUtils.forceMkdirParent(dest);
		// Rewrite the class with the target version
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = new PatcherClassVisitor(cw, cr.getClassName());
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		IOUtils.write(cw.toByteArray(), new FileOutputStream(dest));
	}

	/**
	 * Patcher visitor that downgrades future-versioned content.
	 */
	private class PatcherClassVisitor extends ClassVisitor {
		private final String name;

		public PatcherClassVisitor(ClassVisitor cv, String name) {
			super(Opcodes.ASM9, cv);
			this.name = name;
		}

		private final Set<StringIndyRewriter> stringIndyRewriters = new HashSet<>();

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			// Modify the version
			super.visit(Math.min(version, convertedVersion), access, name, signature, superName, interfaces);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
			// De-Indify strings below Java 9
			if (targetVersion < 9) {
				StringIndyRewriter re = new StringIndyRewriter(mv);
				stringIndyRewriters.add(re);
				return re;
			}
			return mv;
		}

		@Override
		public ModuleVisitor visitModule(String name, int access, String version) {
			if (targetVersion < 9)
				return null;
			return super.visitModule(name, access, version);
		}

		@Override
		public void visitNestHost(String nestHost) {
			if (targetVersion < 9)
				return;
			super.visitNestHost(nestHost);
		}

		@Override
		public void visitNestMember(String nestMember) {
			if (targetVersion < 9)
				return;
			super.visitNestMember(nestMember);
		}

		@Override
		public void visitPermittedSubclass(String permittedSubclass) {
			if (targetVersion < 9)
				return;
			super.visitPermittedSubclass(permittedSubclass);
		}

		@Override
		public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
			if (targetVersion < 9)
				return null;
			return super.visitRecordComponent(name, descriptor, signature);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			int count = stringIndyRewriters.stream()
					.mapToInt(StringIndyRewriter::getReplaced).sum();
			if (count > 0) {
				logger.debug("Replaced " + count + " string indifications in class: " + name);
			}
		}
	}
}
