package software.coley.versionpatcher;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Patcher visitor that downgrades future-versioned content.
 *
 * @author Matt Coley
 */
public class VersionPatcher extends ClassVisitor {
	private final int targetVersion;
	private final int classVersion;
	// State info
	private final List<FieldInfo> fields = new ArrayList<>();
	private String className;
	private boolean rewriteRecordMembers;

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
			rewriteRecordMembers = true;
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
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		// Rewrite record methods
		if (rewriteRecordMembers) {
			// Rewrite constructor to point to 'java/lang/Object' super-class when calling parent '<init>'
			if ("<init>".equals(name))
				mv = new RecordConstructorRewriter(mv);
			// Rewrite implementation methods
			if (RecordMethodImplRewriter.isRecognizedTargetMethod(access, name, descriptor))
				mv = new RecordMethodImplRewriter(mv, className, fields, name, descriptor);
		}
		// Rewrite string concatenation to not use invoke-dynamic
		if (targetVersion < 9)
			mv = new StringIndyRewriter(mv);
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
}
