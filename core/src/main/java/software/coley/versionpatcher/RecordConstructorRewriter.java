package software.coley.versionpatcher;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites a {@code record} class's constructor to point to {@code Object.<init>()V}
 * rather than {@code Record.<init>()V}
 *
 * @author Matt Coley
 */
public class RecordConstructorRewriter extends MethodVisitor {
	/**
	 * @param mv
	 * 		Parent method visitor.
	 */
	public RecordConstructorRewriter(MethodVisitor mv) {
		super(Opcodes.ASM9, mv);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		// Modify super call for records (previewed in 14) in constructors
		if (opcode == Opcodes.INVOKESPECIAL && "<init>".equals(name) && "java/lang/Record".equals(owner))
			owner = "java/lang/Object";
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
	}
}
