package software.coley.versionpatcher;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Patcher visitor that downgrades future-versioned content.
 */
public class VersionPatcher extends ClassVisitor {
	private final int targetVersion;
	private final int classVersion;
	// State info
	private final List<FieldInfo> fields = new ArrayList<>();
	private String className;
	private boolean wasRecord;

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
			wasRecord = true;
			superName = "java/lang/Object";
			access &= ~Opcodes.ACC_RECORD;
		}
		className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		fields.add(new FieldInfo(access, name, descriptor));
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
		MethodVisitor mv = new MethodPatcher(parent, name);
		// Rewrite record implementation methods
		if (wasRecord && RecordMethodImplRewriter.isRecognizedTargetMethod(access, name, descriptor)) {
			mv = new RecordMethodImplRewriter(mv, className, fields, name, descriptor);
		}
		// De-Indify strings below Java 9
		if (targetVersion < 9) {
			mv = new StringIndyRewriter(mv);
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
		if (targetVersion < 11)
			return;
		super.visitNestHost(nestHost);
	}

	@Override
	public void visitNestMember(String nestMember) {
		if (targetVersion < 11)
			return;
		super.visitNestMember(nestMember);
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		if (targetVersion < 15)
			return;
		super.visitPermittedSubclass(permittedSubclass);
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		if (targetVersion < 14)
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
