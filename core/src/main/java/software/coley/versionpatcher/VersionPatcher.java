package software.coley.versionpatcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;

/**
 * Patcher visitor that downgrades future-versioned content.
 */
public class VersionPatcher extends ClassVisitor {
	private final int targetVersion;
	private final int classVersion;

	public VersionPatcher(ClassVisitor parent, int targetVersion) {
		super(Opcodes.ASM9, parent);
		this.targetVersion = targetVersion;
		classVersion = 44 + targetVersion;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// Modify the version
		version = Math.min(version, classVersion);
		// Modify super-type for records (previewed in 14)
		if (targetVersion < 14 && "java/lang/Record".equals(superName)) {
			superName = "java/lang/Object";
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
		MethodVisitor mv = new MethodPatcher(parent, name);
		// De-Indify strings below Java 9
		if (targetVersion < 9) {
			return new StringIndyRewriter(mv);
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

	class MethodPatcher extends MethodVisitor {
		private final String methodName;

		public MethodPatcher(MethodVisitor mv, String methodName) {
			super(Opcodes.ASM9, mv);
			this.methodName = methodName;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			// Modify super call for records (previewed in 14) in constructors
			if (methodName.equals("<init>") && opcode == Opcodes.INVOKESPECIAL &&
					targetVersion < 14 && "java/lang/Record".equals(owner)) {
				owner = "java/lang/Object";
			}
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
