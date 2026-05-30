package fun.efto.luna.core.bytecode.asm.assembler;

import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import org.objectweb.asm.Type;

public interface ExpressionSegment {

    class StringSegment implements ExpressionSegment {
        private final String text;
        public StringSegment(String text) { this.text = text; }
        public String getText() { return text; }
    }

    class ParameterSegment implements ExpressionSegment {
        private final Type type;
        private final int slot;
        public ParameterSegment(Type type, int slot) { this.type = type; this.slot = slot; }
        public Type getType() { return type; }
        public int getSlot() { return slot; }
    }

    class LocalVariableSegment implements ExpressionSegment {
        private final String name;
        private final String descriptor;
        private final int slot;
        public LocalVariableSegment(String name, String descriptor, int slot) {
            this.name = name;
            this.descriptor = descriptor;
            this.slot = slot;
        }
        public String getName() { return name; }
        public String getDescriptor() { return descriptor; }
        public int getSlot() { return slot; }
    }
}
