package fun.efto.luna.core.injector.impl;

/**
 * 方法退出注入器
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injector.BytecodeInjector;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.target.MethodTarget;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ExitMethodInjector implements BytecodeInjector {

    @Override
    public byte[] inject(InjectionContext injectionContext, byte[] bytecode, BytecodeAssembler bytecodeAssembler) {
        ClassReader cr = new ClassReader(bytecode);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cr.accept(new MethodInjectorClassVisitor(cw, injectionContext), ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private static class MethodInjectorClassVisitor extends org.objectweb.asm.ClassVisitor {
        private final InjectionContext injectionContext;

        public MethodInjectorClassVisitor(ClassWriter cw, InjectionContext injectionContext) {
            super(Opcodes.ASM9, cw);
            this.injectionContext = injectionContext;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            InjectionPoint injectionPoint = injectionContext.getInjectionPoint();
            if (injectionPoint.getTarget() instanceof MethodTarget) {
                MethodTarget methodTarget = (MethodTarget) injectionPoint.getTarget();
                if (methodTarget.getMethodName().equals(name) && methodTarget.getMethodDescriptor().equals(descriptor)) {
                    return new ExitMethodInjectorMethodVisitor(mv, injectionPoint);
                }
            }
            return mv;
        }
    }

    private static class ExitMethodInjectorMethodVisitor extends MethodVisitor {
        private final InjectionPoint injectionPoint;

        public ExitMethodInjectorMethodVisitor(MethodVisitor mv, InjectionPoint injectionPoint) {
            super(Opcodes.ASM9, mv);
            this.injectionPoint = injectionPoint;
        }

        @Override
        public void visitInsn(int opcode) {
            // 在方法返回指令前注入日志代码
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                // 注入方法退出时的日志代码
                mv.visitFieldInsn(Opcodes.GETSTATIC, "org/apache/logging/log4j/LogManager", "ROOT_LOGGER_NAME", "Ljava/lang/String;");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/apache/logging/log4j/LogManager", "getLogger", "(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;", false);
                mv.visitLdcInsn("Exiting method: " + injectionPoint.getTarget().getMethodName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info", "(Ljava/lang/String;)V", true);
            }
            super.visitInsn(opcode);
        }
    }
}
