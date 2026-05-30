package fun.efto.luna.core.analyzer;

/**
 * 异常表条目
 * 对应字节码中 try-catch 块的异常处理信息
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/26 00:00
 */
public class ExceptionTableEntry {
    private final int startPc;
    private final int endPc;
    private final int handlerPc;
    private final String catchType;

    public ExceptionTableEntry(int startPc, int endPc, int handlerPc, String catchType) {
        this.startPc = startPc;
        this.endPc = endPc;
        this.handlerPc = handlerPc;
        this.catchType = catchType;
    }

    public int getStartPc() { return startPc; }
    public int getEndPc() { return endPc; }
    public int getHandlerPc() { return handlerPc; }
    public String getCatchType() { return catchType; }
}