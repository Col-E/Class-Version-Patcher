package software.coley.versionpatcher;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A method visitor that replaces Java 14+ record method implementations for:
 * <ul>
 *     <li>{@link Object#hashCode()}</li>
 *     <li>{@link Object#equals(Object)}</li>
 *     <li>{@link Object#toString()}</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class RecordMethodImplRewriter extends MethodVisitor implements Opcodes {
	private final List<FieldInfo> fields;
	private final String declaringType;
	private final String methodName;
	private final String methodDesc;

	/**
	 * @param mv
	 * 		Parent method visitor.
	 * @param declaringType
	 * 		Name of record declaration.
	 * @param fields
	 * 		Declared fields in the class.
	 * @param methodName
	 * 		Name of the method being rewritten.
	 * @param methodDesc
	 * 		Descriptor of the method being rewritten.
	 */
	public RecordMethodImplRewriter(MethodVisitor mv, String declaringType, List<FieldInfo> fields, String methodName, String methodDesc) {
		super(Opcodes.ASM9, mv);
		this.declaringType = declaringType;
		this.fields = fields;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
	}

	/**
	 * @param access
	 * 		Method access flags.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return {@code true} when the given is one targeted by {@link RecordMethodImplRewriter}.
	 */
	public static boolean isRecognizedTargetMethod(int access, String name, String descriptor) {
		return access == (Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC) &&
				(isToString(name, descriptor) || isHashCode(name, descriptor) || isEquals(name, descriptor));
	}

	private static boolean isEquals(String name, String descriptor) {
		return name.equals("equals") && descriptor.equals("(Ljava/lang/Object;)Z");
	}

	private static boolean isHashCode(String name, String descriptor) {
		return name.equals("hashCode") && descriptor.equals("()I");
	}

	private static boolean isToString(String name, String descriptor) {
		return name.equals("toString") && descriptor.equals("()Ljava/lang/String;");
	}


	@Override
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitInsn(int opcode) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}


	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitLabel(Label label) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitLdcInsn(Object value) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
		return null;
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
		return null;
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
		return null;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// Don't visit anything.
		// Rewriting is handled in visitEnd()
	}

	@Override
	public void visitEnd() {
		int maxLocals;
		int maxStack;
		List<Runnable> variablePopulators = new ArrayList<>();
		Label start = new Label();
		Label end = new Label();
		mv.visitLabel(start);
		variablePopulators.add(() -> mv.visitLocalVariable("this", "L" + declaringType + ";", null, start, end, 0));
		if (isEquals(methodName, methodDesc)) {
			Label passEquals = new Label();
			Label passNull = new Label();
			Label passClass = new Label();
			Label castStart = new Label();
			Label successReturn = new Label();
			Label fallbackReturn = new Label();
			variablePopulators.add(() -> mv.visitLocalVariable("o", "Ljava/lang/Object;", null, start, end, 1));
			variablePopulators.add(() -> mv.visitLocalVariable("other", "L" + declaringType + ";", null, castStart, end, 2));
			// if (this == o) return true;
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitJumpInsn(IF_ACMPNE, passEquals);
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IRETURN);
			mv.visitLabel(passEquals);
			// if (o == null) return false;
			mv.visitVarInsn(ALOAD, 1);
			mv.visitJumpInsn(IFNONNULL, passNull);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			mv.visitLabel(passNull);
			// if (getClass() != o.getClass()) return false;
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitJumpInsn(IF_ACMPEQ, passClass);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			mv.visitLabel(passClass);
			// Type other = (Type) o;
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, declaringType);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitLabel(castStart);
			// Compare all fields for equality
			for (FieldInfo field : fields) {
				// push our field
				pushFieldValueToStack(field);
				mapStackTopValueToObject(field.getDescriptor());
				// push other field
				mv.visitVarInsn(ALOAD, 2);
				mv.visitFieldInsn(GETFIELD, declaringType, field.getName(), field.getDescriptor());
				mapStackTopValueToObject(field.getDescriptor());
				// Compare via Objects.equals(a, b)
				//  pushes 0 when compare fails
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
				mv.visitJumpInsn(IFEQ, fallbackReturn);
			}
			// All comparisons have passed
			mv.visitLabel(successReturn);
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IRETURN);
			// Fallback, return false
			mv.visitLabel(fallbackReturn);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			maxLocals = 3;
			maxStack = 2;
		} else if (isHashCode(methodName, methodDesc)) {
			// Create Object[number-of-fields]
			mv.visitIntInsn(BIPUSH, fields.size());
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			// array[n] = fields[n]
			for (int i = 0; i < fields.size(); i++) {
				FieldInfo field = fields.get(i);
				mv.visitInsn(DUP);
				mv.visitIntInsn(BIPUSH, i);
				pushFieldValueToStack(field);
				mapStackTopValueToObject(field.getDescriptor());
				mv.visitInsn(AASTORE);
			}
			// return Objects.hash(array)
			mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hash", "([Ljava/lang/Object;)I", false);
			mv.visitInsn(IRETURN);
			maxLocals = 1;
			maxStack = 5;
		} else if (isToString(methodName, methodDesc)) {
			String simpleTypeName = declaringType.substring(declaringType.lastIndexOf('/') + 1);
			// Create pattern: Type[key=value, key=value]
			//  - using StringBuilder
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
			if (fields.isEmpty()) {
				mv.visitLdcInsn(simpleTypeName + "[]");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			} else {
				for (int i = 0; i < fields.size(); i++) {
					FieldInfo field = fields.get(i);
					boolean isFirst = i == 0;
					if (isFirst) {
						mv.visitLdcInsn(simpleTypeName + "[" + field.getName() + "=");
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
						pushFieldValueToStack(field);
						mapStackTopValueToObject(field.getDescriptor());
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
					} else {
						mv.visitLdcInsn(", " + field.getName() + "=");
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
						pushFieldValueToStack(field);
						mapStackTopValueToObject(field.getDescriptor());
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
					}
				}
				mv.visitLdcInsn("]");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			}
			// return sb.toString();
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			mv.visitInsn(ARETURN);
			maxLocals = 1;
			maxStack = 3;
		} else {
			throw new IllegalStateException("Unsupported method: " + declaringType + "." + methodName + methodName);
		}
		mv.visitLabel(end);
		variablePopulators.forEach(Runnable::run);
		mv.visitMaxs(maxStack, maxLocals);
		super.visitEnd();
	}

	private void pushFieldValueToStack(FieldInfo field) {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, declaringType, field.getName(), field.getDescriptor());
	}

	private void mapStackTopValueToObject(String stackTopDescriptor) {
		Type type = Type.getType(stackTopDescriptor);
		switch (type.getSort()) {
			case Type.BOOLEAN:
				// Boolean.valueOf(true/false)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
				break;
			case Type.CHAR:
				// Character.valueOf(char)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				break;
			case Type.BYTE:
				// Byte.valueOf(byte)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				break;
			case Type.SHORT:
				// Short.valueOf(short)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				break;
			case Type.INT:
				// Integer.valueOf(int)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				break;
			case Type.FLOAT:
				// Float.valueOf(float)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				break;
			case Type.DOUBLE:
				// Double.valueOf(double)
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				break;
			case Type.ARRAY:
			case Type.OBJECT:
			default:
				// No conversion needed
				break;
		}
	}
}
