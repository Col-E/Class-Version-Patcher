package software.coley.versionpatcher.gradle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.type.ArtifactTypeContainer;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.versionpatcher.StringCompatDumper;
import software.coley.versionpatcher.VersionPatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gradle plugin implementation for patching.
 *
 * @author Matt Coley
 */
public class PatcherPlugin implements Plugin<Project> {
	private static final Logger logger = LoggerFactory.getLogger(PatcherPlugin.class);

	@Override
	public void apply(Project project) {
		PatcherExtension extension = project.getExtensions().create("patcherConfig", PatcherExtension.class, project);
		configureCompilePatching(project, extension);
		configureDependencyPatching(project, extension);
		configurePostProcessing(project, extension);
	}

	private void configurePostProcessing(Project project, PatcherExtension extension) {
		// Skip if code patching is disabled
		if (!extension.getPostProcess().get()) {
			return;
		}
		Task taskPrcoess = project.getTasks().register("patch-processing").get();
		taskPrcoess.setActions(Collections.singletonList(task -> {
			addPostProcessing(project, extension.getTargetVersion().getOrElse(8));
		}));
		// Inject code patching after compilation step
		taskPrcoess.setDependsOn(project.getTasksByName("compileJava", false));
	}

	private void configureCompilePatching(Project project, PatcherExtension extension) {
		// Skip if code patching is disabled
		if (!extension.getPatchCode().get()) {
			return;
		}
		Task taskPatchCode = project.getTasks().register("patch-code").get();
		taskPatchCode.setActions(Collections.singletonList(task -> {
			patchCode(project, extension.getTargetVersion().getOrElse(8));
		}));
		// Inject code patching after compilation step
		taskPatchCode.setDependsOn(project.getTasksByName("compileJava", false));
	}

	private void configureDependencyPatching(Project project, PatcherExtension extension) {
		// Skip if dependency patching is disabled
		if (!extension.getPatchDependencies().get()) {
			return;
		}
		Task taskPatchDependencies = project.getTasks().register("patch-deps").get();
		taskPatchDependencies.setActions(Collections.singletonList(task -> {
			patchDependencies(project, extension.getArtifacts().getOrElse(Collections.emptyList()));
		}));
		// Inject dependency patching before compilation step
		project.getTasksByName("compileJava", false).forEach(compileTask -> {
			Set<Object> dependentTasks = new HashSet<>(compileTask.getDependsOn());
			dependentTasks.add(taskPatchDependencies);
			compileTask.setDependsOn(dependentTasks);
		});
	}

	private void patchDependencies(Project project, List<String> artifacts) {
		 // TODO: Scan dependencies and copy complaint versions to output directory
	}

	private void patchCode(Project project, int targetVersion) {
		// Collect classes to patch
		File buildDir = project.getBuildDir();
		Collection<Path> classes;
		try {
			classes = Files.walk(Paths.get(buildDir.getAbsolutePath()))
					.filter(path -> path.getFileName().toString().endsWith(".class"))
					.collect(Collectors.toList());
		} catch (IOException ex) {
			logger.error("Failed to collect paths of classes to patch", ex);
			return;
		}
		// Patch each class
		for (Path path : classes) {
			try {
				ClassReader cr = new ClassReader(Files.readAllBytes(path));
				ClassWriter cw = new ClassWriter(0);
				ClassVisitor cv = new VersionPatcher(cw, targetVersion);
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
				IOUtils.write(cw.toByteArray(), new FileOutputStream(path.toFile()));
			} catch (Exception ex) {
				logger.error("Failed to patch class '" + path.getFileName().toString() + "'", ex);
			}
		}
	}

	private void addPostProcessing(Project project, int targetVersion) {
		if (targetVersion < 9) {
			// TODO: Drop extra classes in output dir
		}
	}
}
