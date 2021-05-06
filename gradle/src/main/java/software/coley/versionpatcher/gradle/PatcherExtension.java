package software.coley.versionpatcher.gradle;

import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Plugin config properties.
 *
 * @author Matt Coley
 */
public class PatcherExtension {
	private final Property<Integer> targetVersion;
	private final Property<Boolean> patchDependencies;
	private final Property<Boolean> patchCode;
	private final Property<Boolean> postProcess;
	private final ListProperty<String> artifacts;

	public PatcherExtension(Project project) {
		ObjectFactory objects = project.getObjects();
		this.targetVersion = objects.property(Integer.class);
		this.patchDependencies = objects.property(Boolean.class);
		this.patchCode = objects.property(Boolean.class);
		this.postProcess = objects.property(Boolean.class);
		this.artifacts = objects.listProperty(String.class);
	}

	public Property<Integer> getTargetVersion() {
		return targetVersion;
	}

	public Property<Boolean> getPatchDependencies() {
		return patchDependencies;
	}

	public Property<Boolean> getPatchCode() {
		return patchCode;
	}

	public Property<Boolean> getPostProcess() {
		return postProcess;
	}

	public ListProperty<String> getArtifacts() {
		return artifacts;
	}
}
