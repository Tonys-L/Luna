package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.rule.InjectionRule;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionLocation;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
class InjectionLocationRenameTest {

    @Test
    @DisplayName("InjectionLocation 类存在且可实例化（通过 of 工厂方法）")
    void injectionLocationClassExists() {
        InjectionLocation location = InjectionLocation.of("test", "test description");
        assertNotNull(location);
        assertEquals("test", location.getName());
        assertEquals("test description", location.getDescription());
    }

    @Test
    @DisplayName("MethodInjectionLocation 类存在且可访问常量")
    void methodInjectionLocationClassExists() {
        assertNotNull(MethodInjectionLocation.ENTER);
        assertNotNull(MethodInjectionLocation.EXIT);
        assertNotNull(MethodInjectionLocation.AROUND);
        assertEquals("method_enter", MethodInjectionLocation.ENTER.getName());
    }

    @Test
    @DisplayName("LineNumberInjectionLocation 类存在且可访问常量")
    void lineNumberInjectionLocationClassExists() {
        assertNotNull(LineNumberInjectionLocation.BEFORE);
        assertNotNull(LineNumberInjectionLocation.AFTER);
        assertEquals("line_before", LineNumberInjectionLocation.BEFORE.getName());
    }

    @Test
    @DisplayName("InjectionTarget.getLocation() 方法存在")
    void injectionTargetGetLocationMethodExists() throws NoSuchMethodException {
        Method getLocation = InjectionTarget.class.getMethod("getLocation");
        assertNotNull(getLocation);
        assertEquals(InjectionLocation.class, getLocation.getReturnType());
    }

    @Test
    @DisplayName("InjectionPoint.getInjectionLocation() 方法存在")
    void injectionPointGetInjectionLocationMethodExists() throws NoSuchMethodException {
        Method getInjectionLocation = InjectionPoint.class.getMethod("getInjectionLocation");
        assertNotNull(getInjectionLocation);
        assertEquals(InjectionLocation.class, getInjectionLocation.getReturnType());
    }

    @Test
    @DisplayName("InjectionRule 使用 injectionLocation 字段（不再有 injectionType）")
    void injectionRuleUsesInjectionLocation() throws NoSuchMethodException {
        InjectionRule rule = new InjectionRule();
        rule.setInjectionLocation("method_enter");
        assertEquals("method_enter", rule.getInjectionLocation());

        assertThrows(NoSuchMethodException.class, () ->
            InjectionRule.class.getMethod("getInjectionType"));
        assertThrows(NoSuchMethodException.class, () ->
            InjectionRule.class.getMethod("setInjectionType", String.class));
    }

    @Test
    @DisplayName("PersistentInjection 使用 injectionLocation 字段（不再有 injectionType）")
    void persistentInjectionUsesInjectionLocation() throws NoSuchMethodException {
        PersistentInjection pi = new PersistentInjection();
        pi.setInjectionLocation("method_enter");
        assertEquals("method_enter", pi.getInjectionLocation());

        assertThrows(NoSuchMethodException.class, () ->
            PersistentInjection.class.getMethod("getInjectionType"));
        assertThrows(NoSuchMethodException.class, () ->
            PersistentInjection.class.getMethod("setInjectionType", String.class));
    }

    @Test
    @DisplayName("InjectionCommand 使用 injectionLocation 字段（不再有 injectionType）")
    void injectionCommandUsesInjectionLocation() throws NoSuchMethodException {
        InjectionCommand cmd = new InjectionCommand();
        cmd.setInjectionLocation("method_enter");
        assertEquals("method_enter", cmd.getInjectionLocation());

        assertThrows(NoSuchMethodException.class, () ->
            InjectionCommand.class.getMethod("getInjectionType"));
        assertThrows(NoSuchMethodException.class, () ->
            InjectionCommand.class.getMethod("setInjectionType", String.class));
    }
}
