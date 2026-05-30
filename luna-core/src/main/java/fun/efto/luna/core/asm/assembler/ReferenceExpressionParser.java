package fun.efto.luna.core.asm.assembler;

import fun.efto.luna.core.asm.AsmInjectionContext.LocalVarInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReferenceExpressionParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceExpressionParser.class);
    private static final Pattern COMBINED_REF_PATTERN = Pattern.compile("\\$(\\d+|[a-zA-Z_]\\w*)");

    public static List<ExpressionSegment> parseExpression(String expression, String methodDescriptor, boolean isStatic,
                                                           List<LocalVarInfo> localVars, List<LocalVarInfo> excludedSameLineVars) {
        List<ExpressionSegment.ParameterSegment> params = parseMethodParams(methodDescriptor, isStatic);
        List<ExpressionSegment> segments = new ArrayList<>();
        Matcher matcher = COMBINED_REF_PATTERN.matcher(expression);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                segments.add(new ExpressionSegment.StringSegment(expression.substring(lastEnd, matcher.start())));
            }
            String ref = matcher.group(1);

            if (ref.matches("\\d+")) {
                int paramIndex = Integer.parseInt(ref);
                if (paramIndex < 1 || paramIndex > params.size()) {
                    LOGGER.warn("[ReferenceExpressionParser] Unresolved parameter reference: ${}, method has {} parameter(s). "
                                    + "Possible cause: methodDescriptor is empty, cannot resolve parameter types. "
                                    + "Ensure the injection rule includes methodDescriptor.",
                            "$" + ref, params.size());
                    segments.add(new ExpressionSegment.StringSegment("$" + ref));
                } else {
                    segments.add(params.get(paramIndex - 1));
                }
            } else {
                LocalVarInfo matched = null;
                if (localVars != null) {
                    for (LocalVarInfo lv : localVars) {
                        if (lv.getName().equals(ref)) {
                            matched = lv;
                            break;
                        }
                    }
                }
                if (matched == null) {
                    boolean isSameLineVar = excludedSameLineVars != null
                            && excludedSameLineVars.stream().anyMatch(v -> v.getName().equals(ref));
                    if (isSameLineVar) {
                        LOGGER.warn("[ReferenceExpressionParser] Unresolved variable: ${} - variable is declared on the same line as LINE_BEFORE injection point. "
                                        + "At LINE_BEFORE, this variable has NOT been initialized yet. "
                                        + "Use LINE_AFTER injection or reference it from a later line. "
                                        + "Available safe vars: [{}]",
                                ref, localVars != null ? localVars.stream().map(LocalVarInfo::getName).collect(Collectors.joining(", ")) : "(none)");
                    } else {
                        String availableVars = localVars != null
                                ? localVars.stream().map(LocalVarInfo::getName).collect(Collectors.joining(", "))
                                : "(none)";
                        LOGGER.warn("[ReferenceExpressionParser] Unresolved variable reference: ${}, available local vars: [{}]. "
                                        + "Possible causes: 1) variable not in scope at injection point, "
                                        + "2) class compiled without debug info (-g:none). "
                                        + "Use $1, $2... for method parameters.",
                                ref, availableVars);
                    }
                    segments.add(new ExpressionSegment.StringSegment("$" + ref));
                } else {
                    segments.add(new ExpressionSegment.LocalVariableSegment(matched.getName(), matched.getDescriptor(), matched.getSlot()));
                }
            }
            lastEnd = matcher.end();
        }

        if (lastEnd < expression.length()) {
            segments.add(new ExpressionSegment.StringSegment(expression.substring(lastEnd)));
        }

        if (segments.isEmpty()) {
            segments.add(new ExpressionSegment.StringSegment(expression));
        }

        return segments;
    }

    public static List<ExpressionSegment.ParameterSegment> parseMethodParams(String methodDescriptor, boolean isStatic) {
        List<ExpressionSegment.ParameterSegment> params = new ArrayList<>();
        if (methodDescriptor == null || methodDescriptor.isEmpty()) {
            return params;
        }

        Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
        int slot = isStatic ? 0 : 1;

        for (Type argType : argTypes) {
            params.add(new ExpressionSegment.ParameterSegment(argType, slot));
            slot += argType.getSize();
        }

        return params;
    }
}
