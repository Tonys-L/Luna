package fun.efto.luna.core.expression.bytecode;

import java.util.HashMap;
import java.util.Map;

/**
 * 变量槽位解析器，用于解析变量名到槽位的映射
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class VariableSlotResolver {

    private final Map<String, Integer> variableSlots;

    public VariableSlotResolver() {
        this.variableSlots = new HashMap<>();
    }

    /**
     * 注册变量及其对应的槽位
     * @param variableName 变量名
     * @param slot 槽位索引
     */
    public void registerVariable(String variableName, int slot) {
        variableSlots.put(variableName, slot);
    }

    /**
     * 获取变量对应的槽位
     * @param variableName 变量名
     * @return 槽位索引，如果变量未注册则返回 -1
     */
    public int getSlot(String variableName) {
        Integer slot = variableSlots.get(variableName);
        return slot != null ? slot : -1;
    }

    /**
     * 检查变量是否已注册
     * @param variableName 变量名
     * @return 是否已注册
     */
    public boolean containsVariable(String variableName) {
        return variableSlots.containsKey(variableName);
    }

    /**
     * 获取所有变量槽位映射
     * @return 变量槽位映射
     */
    public Map<String, Integer> getVariableSlots() {
        return new HashMap<>(variableSlots);
    }
}
