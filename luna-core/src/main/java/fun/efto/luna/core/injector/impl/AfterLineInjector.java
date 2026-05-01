package fun.efto.luna.core.injector.impl;

/**
 * 行后注入器
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injector.BytecodeInjector;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AfterLineInjector implements BytecodeInjector {

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        ClassReader cr = new ClassReader(bytecode);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cr.accept(new LineInjectorClassVisitor(cw, injectionContext), ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private static class LineInjectorClassVisitor extends org.objectweb.asm.ClassVisitor {
        private final InjectionContext injectionContext;

        public LineInjectorClassVisitor(ClassWriter cw, InjectionContext injectionContext) {
            super(Opcodes.ASM9, cw);
            this.injectionContext = injectionContext;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            InjectionPoint injectionPoint = injectionContext.getInjectionPoint();
            if (injectionPoint.getTarget() instanceof LineNumberTarget) {
                LineNumberTarget lineTarget = (LineNumberTarget) injectionPoint.getTarget();
                return new AfterLineInjectorMethodVisitor(mv, injectionPoint);
            }
            return mv;
        }
    }

    private static class AfterLineInjectorMethodVisitor extends MethodVisitor {
        private final InjectionPoint injectionPoint;
        private int currentLine = -1;
        private org.objectweb.asm.Label lastLabel = null;

        public AfterLineInjectorMethodVisitor(MethodVisitor mv, InjectionPoint injectionPoint) {
            super(Opcodes.ASM9, mv);
            this.injectionPoint = injectionPoint;
        }

        @Override
        public void visitLineNumber(int line, org.objectweb.asm.Label start) {
            LineNumberTarget lineTarget = (LineNumberTarget) injectionPoint.getTarget();
            // 如果当前行是目标行的下一行，说明目标行已经执行完毕
            if (currentLine == lineTarget.getLineNumber()) {
                // 在目标行后注入日志代码
                mv.visitFieldInsn(Opcodes.GETSTATIC, "org/apache/logging/log4j/LogManager", "ROOT_LOGGER_NAME", "Ljava/lang/String;");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/apache/logging/log4j/LogManager", "getLogger", "(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;", false);
                mv.visitLdcInsn("After line " + currentLine + " in method: " + injectionPoint.getTarget().getMethodName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info", "(Ljava/lang/String;)V", true);
            }
            currentLine = line;
            lastLabel = start;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitInsn(int opcode) {
            // 处理方法结束时的情况
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                LineNumberTarget lineTarget = (LineNumberTarget) injectionPoint.getTarget();
                if (currentLine == lineTarget.getLineNumber()) {
                    // 在方法结束前注入日志代码
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "org/apache/logging/log4j/LogManager", "ROOT_LOGGER_NAME", "Ljava/lang/String;");
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/apache/logging/log4j/LogManager", "getLogger", "(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;", false);
                    mv.visitLdcInsn("After line " + currentLine + " in method: " + injectionPoint.getTarget().getMethodName());
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info", "(Ljava/lang/String;)V", true);
                }
            }
            super.visitInsn(opcode);
        }
    }
}
