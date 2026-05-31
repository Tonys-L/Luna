package fun.efto.luna.core.injection;

import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.builtin.log.ExpressionCodeEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
class CodeEngineTest {

    @Nested
    @DisplayName("CompiledCode")
    class CompiledCodeTest {

        @Test
        @DisplayName("constructor with condition and content")
        void constructorWithConditionAndContent() {
            CompiledCode code = new CompiledCode("${params[0] > 0}", "log:hello");
            assertEquals("${params[0] > 0}", code.getCondition());
            assertEquals("log:hello", code.getContent());
            assertNull(code.getSegments());
        }

        @Test
        @DisplayName("constructor with segments")
        void constructorWithSegments() {
            java.util.List<ExpressionSegment> segments = Collections.singletonList(new ExpressionSegment.StringSegment("hello"));
            CompiledCode code = new CompiledCode(null, "log:hello", segments);
            assertNull(code.getCondition());
            assertEquals("log:hello", code.getContent());
            assertEquals(1, code.getSegments().size());
        }

        @Test
        @DisplayName("hasCondition returns true when condition is non-empty")
        void hasConditionTrue() {
            CompiledCode code = new CompiledCode("${params[0] > 0}", "log:hello");
            assertTrue(code.hasCondition());
        }

        @Test
        @DisplayName("hasCondition returns false when condition is null")
        void hasConditionFalseNull() {
            CompiledCode code = new CompiledCode(null, "log:hello");
            assertFalse(code.hasCondition());
        }

        @Test
        @DisplayName("hasCondition returns false when condition is empty")
        void hasConditionFalseEmpty() {
            CompiledCode code = new CompiledCode("", "log:hello");
            assertFalse(code.hasCondition());
        }

        @Test
        @DisplayName("hasCondition returns false when condition is whitespace")
        void hasConditionFalseWhitespace() {
            CompiledCode code = new CompiledCode("   ", "log:hello");
            assertFalse(code.hasCondition());
        }
    }

    @Nested
    @DisplayName("CodeEngineRegistry")
    class CodeEngineRegistryTest {

        @BeforeEach
        void setUp() {
            CodeEngineRegistry.getInstance().clear();
        }

        @AfterEach
        void tearDown() {
            CodeEngineRegistry.getInstance().clear();
        }

        @Test
        @DisplayName("register and lookup by codeType")
        void registerAndLookup() {
            ExpressionCodeEngine engine = new ExpressionCodeEngine();
            CodeEngineRegistry.getInstance().register(engine);

            CodeEngine found = CodeEngineRegistry.getInstance().get("EXPRESSION").orElse(null);
            assertNotNull(found);
            assertEquals("EXPRESSION", found.getCodeType());
        }

        @Test
        @DisplayName("lookup is case-insensitive")
        void lookupCaseInsensitive() {
            ExpressionCodeEngine engine = new ExpressionCodeEngine();
            CodeEngineRegistry.getInstance().register(engine);

            assertTrue(CodeEngineRegistry.getInstance().get("expression").isPresent());
            assertTrue(CodeEngineRegistry.getInstance().get("Expression").isPresent());
            assertTrue(CodeEngineRegistry.getInstance().get("EXPRESSION").isPresent());
        }

        @Test
        @DisplayName("lookup returns empty for unknown codeType")
        void lookupUnknown() {
            assertFalse(CodeEngineRegistry.getInstance().get("UNKNOWN").isPresent());
        }

        @Test
        @DisplayName("lookup returns empty for null codeType")
        void lookupNull() {
            assertFalse(CodeEngineRegistry.getInstance().get(null).isPresent());
        }

        @Test
        @DisplayName("unregister removes engine")
        void unregister() {
            ExpressionCodeEngine engine = new ExpressionCodeEngine();
            CodeEngineRegistry.getInstance().register(engine);
            CodeEngineRegistry.getInstance().unregister(engine);

            assertFalse(CodeEngineRegistry.getInstance().get("EXPRESSION").isPresent());
        }
    }

    @Nested
    @DisplayName("ExpressionCodeEngine")
    class ExpressionCodeEngineTest {

        private ExpressionCodeEngine engine;

        @BeforeEach
        void setUp() {
            engine = new ExpressionCodeEngine();
        }

        @Test
        @DisplayName("getCodeType returns EXPRESSION")
        void getCodeType() {
            assertEquals("EXPRESSION", engine.getCodeType());
        }

        @Test
        @DisplayName("compile returns CompiledCode with condition and content")
        void compileWithCondition() {
            PersistentInjection pi = new PersistentInjection();
            pi.setCodeType("LOG");
            pi.setCode("hello world");
            pi.setExpression("params[0] > 0");

            CompiledCode compiled = engine.compile(pi);

            assertEquals("${params[0] > 0}", compiled.getCondition());
            assertTrue(compiled.getContent().contains("log:hello world"));
            assertTrue(compiled.hasCondition());
        }

        @Test
        @DisplayName("compile without condition")
        void compileWithoutCondition() {
            PersistentInjection pi = new PersistentInjection();
            pi.setCodeType("LOG");
            pi.setCode("hello world");

            CompiledCode compiled = engine.compile(pi);

            assertFalse(compiled.hasCondition());
            assertEquals("log:hello world", compiled.getContent());
        }

        @Test
        @DisplayName("compile with null code defaults to log:")
        void compileWithNullCode() {
            PersistentInjection pi = new PersistentInjection();
            pi.setCodeType("LOG");
            pi.setCode(null);

            CompiledCode compiled = engine.compile(pi);

            assertEquals("log:", compiled.getContent());
        }
    }
}
