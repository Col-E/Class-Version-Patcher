package software.coley.versionpatcher;

/**
 * Wrapper of field declaration information.
 *
 * @author Matt Coley
 */
public class FieldInfo {
	private final int access;
	private final String name;
	private final String descriptor;

	/**
	 * @param access
	 * 		Field modifiers.
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 */
	public FieldInfo(int access, String name, String descriptor) {
		this.access = access;
		this.name = name;
		this.descriptor = descriptor;
	}

	/**
	 * @return Field modifiers.
	 */
	public int getAccess() {
		return access;
	}

	/**
	 * @return Field name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Field descriptor.
	 */
	public String getDescriptor() {
		return descriptor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (getClass() != o.getClass()) return false;
		FieldInfo fieldInfo = (FieldInfo) o;
		return access == fieldInfo.access &&
				name.equals(fieldInfo.name) &&
				descriptor.equals(fieldInfo.descriptor);
	}

	@Override
	public int hashCode() {
		int result = 1;

		result = 31 * result + access;
		result = 31 * result + name.hashCode();
		result = 31 * result + descriptor.hashCode();

		return result;
	}
}
