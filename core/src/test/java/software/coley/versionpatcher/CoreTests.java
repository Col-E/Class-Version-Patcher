package software.coley.versionpatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.*;

/**
 * Load some Java 9+ version classes compiled with a later {@code javac} and run them through the patcher.
 * They should be able to be loaded and run in our Java 8 environment.
 */
@RunWith(Parameterized.class)
public class CoreTests {
	private static final int CLASSFILE_VERSION = (int) Double.parseDouble(System.getProperty("java.class.version"));
	private static final File testResourceDirectory = new File("src" + File.separator + "test" + File.separator + "resources");
	private static final PatchedClassLoader loader = new PatchedClassLoader(CLASSFILE_VERSION - 44, ClassLoader.getSystemClassLoader());

    private final File targetFile;

    public CoreTests(File targetFile) {
        this.targetFile = targetFile;
    }

    @Before
	public void setup() {
		// Assert the test is run on Java 8 or lower to prove that
        // it can downsample classes by running them
		assertTrue(
				"Must run test on a version lower or equal to Java 8!",
				CLASSFILE_VERSION <= Opcodes.V1_8
		);
	}

	@Test
	public void test() {
		// Get name of class from file path
		String className = targetFile.getName();
		className = className.substring(0, className.length() - ".class".length());

		// They should all have main methods, invoke them!
		try {
			Class<?> cls = loader.findClass(className);
			Method main = cls.getDeclaredMethod("main", String[].class);
			main.setAccessible(true);
			String[] args = new String[0];
			main.invoke(null, (Object) args);
		} catch (Exception ex) {
			throw new RuntimeException("Failed while running test for " + className, ex);
		}
	}

    @Parameterized.Parameters
    public static File[] provideFiles() {
        try {
            return testResourceDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches("Java\\d+\\w+.class");
                }
            });
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
				File file = new File(testResourceDirectory, name + ".class");
				if (file.exists()) {
					InputStream is = new FileInputStream(file);
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[4];
					while ((nRead = is.read(data, 0, data.length)) != -1) {
						buffer.write(data, 0, nRead);
					}
					buffer.flush();
					byte[] byteBuffer = buffer.toByteArray();

					ClassReader reader = new ClassReader(byteBuffer);
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
					CheckClassAdapter checker = new CheckClassAdapter(writer);

					VersionPatcher patcher = new VersionPatcher(checker, version);
					reader.accept(patcher, ClassReader.EXPAND_FRAMES);

					byte[] classBytes = writer.toByteArray();
					if (classBytes != null) {
						return defineClass(name, classBytes, 0, classBytes.length);
					}
				}
				// Check if it's something we may drop into the compile output
				if (name.equals(StringCompatDumper.CLASS_NAME.replace('/', '.'))) {
					byte[] classBytes = StringCompatDumper.dump();
					return defineClass(name, classBytes, 0, classBytes.length);
				}
				// Default case
				return super.findClass(name);
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}
}
