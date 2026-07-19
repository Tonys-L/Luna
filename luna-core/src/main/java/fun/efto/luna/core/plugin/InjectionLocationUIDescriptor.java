package fun.efto.luna.core.plugin;

/**
 * 注入位置的 UI 展示描述符，由插件在注册 InjectionLocation 时提供。
 * 与领域模型 InjectionLocation 分离，UI 关注点不属于核心层。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/24 01:00
 */
public class InjectionLocationUIDescriptor {

    private final String categoryLabel;
    private final String color;

    public InjectionLocationUIDescriptor(String categoryLabel, String color) {
        this.categoryLabel = categoryLabel;
        this.color = color;
    }

    public String getCategoryLabel() { return categoryLabel; }
    public String getColor() { return color; }
}
