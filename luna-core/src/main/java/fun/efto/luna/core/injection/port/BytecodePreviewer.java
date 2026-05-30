package fun.efto.luna.core.injection.port;

/**
 * BytecodePreviewer（出站端口）。
 * 领域层需要"预览字节码变换效果"的能力，不直接依赖 ASM 或 ClassTransformer。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 18:00
 */
public interface BytecodePreviewer {

    /**
     * 预览字节码变换效果（dry-run），不实际修改运行中的字节码
     *
     * @param injectionId    注入描述 ID
     * @param className      类名
     * @param originalBytes  原始类字节码
     * @return 预览结果
     */
    PreviewResult preview(String injectionId, String className, byte[] originalBytes);

    /**
     * 预览结果（领域对象，不暴露 TransformerResult 等基础设施类型）
     */
    class PreviewResult {
        private final boolean transformed;
        private final int generatedSize;
        private final int originalSize;
        private final String message;

        public PreviewResult(boolean transformed, int generatedSize, int originalSize, String message) {
            this.transformed = transformed;
            this.generatedSize = generatedSize;
            this.originalSize = originalSize;
            this.message = message;
        }

        public boolean isTransformed() { return transformed; }
        public int getGeneratedSize() { return generatedSize; }
        public int getOriginalSize() { return originalSize; }
        public String getMessage() { return message; }
    }
}
