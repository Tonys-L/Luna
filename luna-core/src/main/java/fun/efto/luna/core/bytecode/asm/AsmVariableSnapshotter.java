package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.bytecode.asm.analyzer.LocalVariableScanner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 21:00
 */
public final class AsmVariableSnapshotter implements VariableSnapshotter {

    @Override
    public List<VariableInfo> snapshot(byte[] bytecode, String method, String desc, int line) {
        List<AsmInjectionContext.LocalVarInfo> localVarInfos =
                LocalVariableScanner.scanVisibleLocalVariables(bytecode, method, desc, line);

        if (localVarInfos == null || localVarInfos.isEmpty()) {
            return Collections.emptyList();
        }

        return localVarInfos.stream()
                .map(lv -> new VariableInfo(lv.getName(), lv.getDescriptor(), lv.getSlot()))
                .collect(Collectors.toList());
    }
}
