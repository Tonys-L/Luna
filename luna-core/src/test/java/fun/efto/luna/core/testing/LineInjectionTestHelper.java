package fun.efto.luna.core.testing;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionLocation;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/30 19:00
 */
public final class LineInjectionTestHelper {

    public static class TestTargetService {
        public void processWithPrimitives(byte b, short s, int i, long l, float f, double d, char c, boolean bool) {
            byte localB = b;
            short localS = s;
            int localI = i;
            long localL = l;
            float localF = f;
            double localD = d;
            char localC = c;
            boolean localBool = bool;
            System.out.println("" + localB + localS + localI + localL + localF + localD + localC + localBool);
        }

        public void processWithReferences(String str, Object obj, String[] arr) {
            String localStr = str;
            Object localObj = obj;
            String[] localArr = arr;
            int length = arr != null ? arr.length : 0;
            System.out.println(localStr + localObj + length);
        }

        public void processWithLoop(int count) {
            int sum = 0;
            for (int i = 0; i < count; i++) {
                sum += i;
            }
            int[] data = {1, 2, 3};
            for (int val : data) {
                sum += val;
            }
            System.out.println(sum);
        }

        public void processWithBranch(int x) {
            int result = 0;
            if (x > 0) {
                int positive = x * 2;
                result = positive;
            } else if (x < 0) {
                int negative = x * -1;
                result = negative;
            } else {
                int zero = 0;
                result = zero;
            }
            System.out.println(result);
        }

        public void processWithException() {
            try {
                int risky = 1;
                System.out.println(risky);
            } catch (RuntimeException e) {
                String msg = e.getMessage();
                System.out.println(msg);
            } finally {
                int done = 1;
                System.out.println(done);
            }
        }

        public int processWithMultiReturn(int x) {
            if (x > 100) {
                int big = x * 2;
                return big;
            }
            if (x > 0) {
                int small = x;
                return small;
            }
            int zero = 0;
            return zero;
        }

        public void processWithSync() {
            Object lock = new Object();
            synchronized (lock) {
                int value = 42;
                System.out.println(value);
            }
        }

        public static int staticMethod(int x) {
            int result = x * 10;
            return result;
        }

        public void processWithLongDouble() {
            long bigNum = 10000000000L;
            double precise = 3.141592653589793;
            long computed = bigNum + 1L;
            double scaled = precise * 2.0;
            System.out.println(computed + " " + scaled);
        }
    }

    public static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    private LineInjectionTestHelper() {
    }

    public static byte[] getClassBytecode(Class<?> clazz) {
        String internalName = clazz.getName().replace('.', '/');
        InputStream is = clazz.getClassLoader().getResourceAsStream(internalName + ".class");
        if (is == null) {
            throw new RuntimeException("Cannot find class file: " + internalName);
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            is.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read bytecode for: " + internalName, e);
        }
    }

    public static int findFirstMethodLine(byte[] bytecode, String methodName) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName)) {
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        return ((LineNumberNode) insn).line;
                    }
                }
            }
        }
        return -1;
    }

    public static int findLineByOffset(byte[] bytecode, String methodName, int lineOffset) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName)) {
                int idx = 0;
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        if (idx == lineOffset) {
                            return ((LineNumberNode) insn).line;
                        }
                        idx++;
                    }
                }
            }
        }
        return -1;
    }

    public static List<Integer> getAllMethodLines(byte[] bytecode, String methodName) {
        List<Integer> lines = new ArrayList<>();
        ClassNode cn = new ClassNode();
        new ClassReader(bytecode).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(methodName)) {
                for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LineNumberNode) {
                        lines.add(((LineNumberNode) insn).line);
                    }
                }
                break;
            }
        }
        return lines;
    }

    public static Class<?> injectAndLoad(byte[] bytecode, String className) {
        BytecodeClassLoader loader = new BytecodeClassLoader();
        return loader.defineClass(className, bytecode);
    }

    public static List<ProbeMessage> pollProbeMessages() {
        List<ProbeMessage> messages = new ArrayList<>();
        ProbeMessage msg;
        while ((msg = ProbeOutput.BUFFER.poll()) != null) {
            messages.add(msg);
        }
        return messages;
    }

    public static void clearProbeBuffer() {
        while (ProbeOutput.BUFFER.poll() != null) {
        }
    }

    public static InjectableCode createCode(String codeStr) {
        return new InjectableCode() {
            @Override
            public String getCode() {
                return codeStr;
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };
    }
}
