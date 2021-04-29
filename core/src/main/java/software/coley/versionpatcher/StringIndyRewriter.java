package software.coley.versionpatcher;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A method visitor that replaces Java 9+ string concatenation with {@link StringCompat}.
 *
 * @author Matt Coley
 */
public class StringIndyRewriter extends MethodVisitor implements Opcodes {
	private int replaced;

	public StringIndyRewriter(MethodVisitor mv) {
		super(Opcodes.ASM9, mv);
	}

	/**
	 * @return Number of replaced string indifications.
	 */
	public int getReplaced() {
		return replaced;
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsmHandle, Object... bsmArgs) {
		// Match indy's for the Java 9+ string format
		if (name.equals("makeConcatWithConstants")) {
			// Create the concat replace
			visitTypeInsn(NEW, StringCompatDumper.CLASS_NAME);
			visitInsn(DUP);
			visitMethodInsn(INVOKESPECIAL, StringCompatDumper.CLASS_NAME, "<init>", "()V", false);
			// Visit each arg type in reverse order
			Type targetDesc = Type.getMethodType(descriptor);
			Type[] args = targetDesc.getArgumentTypes();
			for (int i = args.length - 1; i >= 0; i--) {
				Type arg = args[i];
				if (arg.getSize() == 1) {
					// Make sure the argument is on top of the stack with a simple swap
					visitInsn(SWAP);
				} else {
					// Long/double require some more to reorder things.
					// - Insert a copy of the concat below the long/double
					// - Delete the top copy
					visitInsn(DUP_X2);
					visitInsn(POP);
				}
				String compatType = getCompatType(arg);
				visitMethodInsn(INVOKEVIRTUAL, StringCompatDumper.CLASS_NAME, "insert", "(" + compatType + ")" + StringCompatDumper.CLASS_DESCRIPTOR, false);
			}
			// Visit the pattern string and print the value
			visitLdcInsn(bsmArgs[0]);
			visitMethodInsn(INVOKEVIRTUAL, StringCompatDumper.CLASS_NAME, "build", "(Ljava/lang/String;)Ljava/lang/String;", false);
			// Increment count
			replaced++;
		} else {
			super.visitInvokeDynamicInsn(name, descriptor, bsmHandle, bsmArgs);
		}
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		if (replaced > 0)
			maxStack += 2;
		super.visitMaxs(maxStack, maxLocals);
	}

	/**
	 * @param type
	 * 		Input type.
	 *
	 * @return Compatible type for any variant of {@link StringCompat#insert(Object)}.
	 */
	private static String getCompatType(Type type) {
		if (type.getSort() == Type.ARRAY) {
			Type element = type.getElementType();
			if (type.getDimensions() > 1 || element.getSort() >= Type.ARRAY) {
				// Non-primitive array
				return "[Ljava/lang/Object;";
			} else {
				// Primitive array
				return element.getDescriptor();
			}
		} else if (type.getSort() < Type.ARRAY) {
			// Primitive
			return type.getDescriptor();
		}
		// Object
		return "Ljava/lang/Object;";
	}
}
