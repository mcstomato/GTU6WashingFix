package gt6bugfix;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Fixes the server crash:
 *
 *   java.lang.IllegalArgumentException: bound must be positive
 *     at java.util.Random.nextInt(Random.java:388)
 *     at gregapi.util.UT$Code.select(UT.java:1479)
 *     at gregapi.oredict.listeners.OreDictListenerItem_Washing.onTickWorld(...)
 *
 * Original bytecode of select(Object, List) only guards against a null List,
 * so an empty (non-null) List makes Random.nextInt(0) throw. The patched
 * method returns the replacement when the List is null OR empty, which is
 * exactly what every other overload of select already does. No game logic
 * is changed: a non-empty List behaves identically to before.
 */
public class UTSelectTransformer implements IClassTransformer {

	private static final String TARGET_CLASS = "gregapi.util.UT$Code";

	private static final String TARGET_CLASS_INTERNAL = "gregapi/util/UT$Code";
	private static final String TARGET_METHOD = "select";
	private static final String TARGET_METHOD_DESC = "(Ljava/lang/Object;Ljava/util/List;)Ljava/lang/Object;";
	private static final String TARGET_METHOD_SIGNATURE = "<E:Ljava/lang/Object;>(TE;Ljava/util/List<TE;>;)TE;";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !TARGET_CLASS.equals(transformedName)) return basicClass;
		try {
			ClassReader cr = new ClassReader(basicClass);
			final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

			cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
				// Drop the original buggy method...
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					if (TARGET_METHOD.equals(name) && TARGET_METHOD_DESC.equals(desc)) return null;
					return super.visitMethod(access, name, desc, signature, exceptions);
				}

				// ...and emit the fixed one after the class has been fully visited.
				@Override
				public void visitEnd() {
					emitFixedSelect(cv);
					super.visitEnd();
				}
			}, 0);

			return cw.toByteArray();
		} catch (Throwable t) {
			// A broken fix must never break class loading.
			t.printStackTrace();
			return basicClass;
		}
	}

	/**
	 * Equivalent Java source:
	 *
	 * public static <E> E select(E aReplacement, List<E> aList) {
	 *     if (aList == null || aList.isEmpty()) return aReplacement;
	 *     return select(RNGSUS.nextInt(aList.size()), aReplacement, aList);
	 * }
	 */
	private static void emitFixedSelect(ClassVisitor cv) {
		Label lReturnReplacement = new Label();
		Label lCallOverload = new Label();

		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, TARGET_METHOD, TARGET_METHOD_DESC, TARGET_METHOD_SIGNATURE, null);
		mv.visitCode();

		// if (aList == null) return aReplacement;
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitJumpInsn(Opcodes.IFNULL, lReturnReplacement);

		// if (aList.isEmpty()) return aReplacement;
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);
		mv.visitJumpInsn(Opcodes.IFNE, lReturnReplacement);

		// return select(RNGSUS.nextInt(aList.size()), aReplacement, aList);
		mv.visitLabel(lCallOverload);
		mv.visitFieldInsn(Opcodes.GETSTATIC, "gregapi/data/CS", "RNGSUS", "Ljava/util/Random;");
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextInt", "(I)I", false);
		mv.visitInsn(Opcodes.I2L);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, TARGET_CLASS_INTERNAL, TARGET_METHOD, "(JLjava/lang/Object;Ljava/util/List;)Ljava/lang/Object;", false);
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitLabel(lReturnReplacement);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
