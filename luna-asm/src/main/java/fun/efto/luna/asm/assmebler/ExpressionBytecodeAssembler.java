package fun.efto.luna.asm.assmebler;

import fun.efto.luna.asm.AsmInjectionContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/3 18:04
 */
public class ExpressionBytecodeAssembler extends BaseAsmBytecodeAssembler {

    @Override
    protected void doAssemble(AsmInjectionContext asmContext, byte[] bytecode) {
        MethodVisitor mv = asmContext.getMethodVisitor();
        String content = asmContext.getInjectableCode().getContent();
        String[] split = content.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid expression " + content);
        }

        String type = split[0];
        String expression = split[1];
        switch (type) {
            case "log":
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn("method enter ：" + expression);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                break;
            default:
                throw new IllegalArgumentException("unsupported expression " + content);
        }
    }
}