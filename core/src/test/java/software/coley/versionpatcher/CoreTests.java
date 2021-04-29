package software.coley.versionpatcher;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load some Java 9+ version classes compiled with a later {@code javac} and run them through the patcher.
 * They should be able to be loaded and run in our Java 8 environment.
 */
public class CoreTests {
	private static final Path testResourceDirectory = Paths.get("src", "test", "resources");
	private static final PatchedClassLoader loader = new PatchedClassLoader(8, ClassLoader.getSystemClassLoader());

	@BeforeAll
	public static void setup() {
		// Assert the test is run on Java 8 to prove that it can downsample classes by running them
		assertEquals(Double.parseDouble(System.getProperty("java.class.version")), Opcodes.V1_8,
				"Must run test on Java 8!");
	}

	@ParameterizedTest
	@MethodSource("providePaths")
	public void test(Path path) {
		// Get name of class from file path
		String className = path.getFileName().toString();
		className = className.substring(0, className.length() - ".class".length());

		// They should all have main methods, invoke them!
		try {
			Class<?> cls = loader.findClass(className);
			Method main = cls.getDeclaredMethod("main", String[].class);
			main.setAccessible(true);
			String[] args = new String[0];
			main.invoke(null, (Object) args);
		} catch (ReflectiveOperationException ex) {
			fail(ex);
		}
	}

	public static Stream<Path> providePaths() {
		try {
			return Files.walk(testResourceDirectory)
					.filter(p -> p.getFileName().toString().matches("Java\\d+\\w+.class"));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static class PatchedClassLoader extends URLClassLoader {
		private final int version;

		public PatchedClassLoader(int version, ClassLoader parent) {
			super(new URL[0], parent);
			this.version = version;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				Class<?> found = findClass(name);
				if (found != null)
					return found;
			} catch (ClassNotFoundException ignored) {
				// ignored
			}
			return super.loadClass(name);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				// Check if name is one of our test cases
				Path filePath = testResourceDirectory.resolve(name + ".class");
				if (Files.exists(filePath)) {
					ClassReader reader = new ClassReader(Files.readAllBytes(filePath));
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

					VersionPatcher patcher = new VersionPatcher(writer, version);
					reader.accept(patcher, ClassReader.EXPAND_FRAMES);

					byte[] classBytes = writer.toByteArray();
					if (classBytes != null) {
						return defineClass(name, classBytes, 0, classBytes.length);
					}
				}
				// Check if its something we may drop into the compile output
				if (name.equals(StringCompatDumper.CLASS_NAME.replace('/', '.'))) {
					byte[] classBytes = StringCompatDumper.dump();
					return  defineClass(name, classBytes, 0, classBytes.length);
				}
				// Default case
				return super.findClass(name);
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}
}
