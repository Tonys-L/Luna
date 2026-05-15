package fun.efto.luna.core.bytecode;

import fun.efto.luna.core.injection.code.type.CodeType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/01 12:00
 */
public class BytecodeAssemblerRegistryTest {

    @Test
    public void testGetInstance() {
        BytecodeAssemblerRegistry registry = BytecodeAssemblerRegistry.getInstance();
        assertNotNull(registry, "Registry instance should not be null");
    }

    @Test
    public void testGetExpressionAssembler() {
        BytecodeAssemblerRegistry registry = BytecodeAssemblerRegistry.getInstance();
        Optional<BytecodeAssembler> assembler = registry.get(CodeType.EXPRESSION);
        assertTrue(assembler.isPresent(), "EXPRESSION assembler should be present");
    }

    @Test
    public void testGetJavaAssemblerNotRegistered() {
        BytecodeAssemblerRegistry registry = BytecodeAssemblerRegistry.getInstance();
        Optional<BytecodeAssembler> assembler = registry.get(CodeType.JAVA);
        assertFalse(assembler.isPresent(), "JAVA assembler should not be registered yet");
    }
}
