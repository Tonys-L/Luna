package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.InjectionRepository;
import fun.efto.luna.core.injection.InjectionRegistry;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.CompiledCode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public interface ProbeHandler {

    String getProbeType();

    boolean usesCode();

    Set<String> supportedInjectionLocations();

    ValidationResult validate(InjectRequest request);

    void handle(CompiledCode code, GenerateContext ctx);

    /**
     * 注入点删除前的钩子。插件可在此执行关联清理（如成对注入的配对删除）。
     * 在核心删除当前注入点之前调用。插件通过 repository 和 registry 直接操作，
     * 不走 InjectionService.removeInjection()，避免递归。
     *
     * @param injection 即将被删除的注入点
     * @param repository 注入存储（用于查找关联注入）
     * @param registry 注入注册表（用于注销关联注入）
     */
    default void onDelete(PersistentInjection injection, InjectionRepository repository, InjectionRegistry registry) {}

    /**
     * 注入点创建后的钩子。插件可在此执行关联操作（如成对注入的自动创建）。
     * 在核心完成当前注入点的保存和注册之后、触发 retransform 之前调用。
     *
     * @param injection 已创建的注入点
     * @param ctx 插件上下文（用于创建衍生注入等操作）
     */
    default void onInject(PersistentInjection injection, PluginContext ctx) {}

    // 新增：前端展示元数据
    default String getDisplayName() { return getProbeType(); }
    default String getSyntax() { return ""; }

    /**
     * 图标类名，支持 Font Awesome 任意风格（如 "fas fa-print"、"fab fa-github"）。
     */
    default String getIcon() { return ""; }

    default String getCategory() { return "injection"; }

    /**
     * Glyph 颜色，CSS 颜色值（如 "#6366f1"、"rgb(99,102,241)"）。
     * 编辑器左侧标记使用 getIcon() 图标 + 此颜色渲染。
     */
    default String getGlyphColor() { return ""; }

    // 新增：快捷菜单行为
    default QuickActionBehavior getQuickActionBehavior() {
        return getConfigSchema().isEmpty() ? QuickActionBehavior.DIRECT : QuickActionBehavior.FORM;
    }

    // 新增：配置表单 schema
    default List<FormFieldSchema> getConfigSchema() { return Collections.emptyList(); }

    /**
     * 是否需要在 EXIT 阶段捕获方法返回值。
     * 返回 true 时，注入器会为非 void 方法生成 DUP+box+ASTORE pre-code，
     * 并通过 AsmInjectionContext.returnCaptureSlot 传递给探针处理器。
     */
    default boolean capturesReturnValue() { return false; }
}
