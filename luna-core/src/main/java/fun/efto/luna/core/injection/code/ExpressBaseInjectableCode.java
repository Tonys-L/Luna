package fun.efto.luna.core.injection.code;

import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.code.type.ExpressionCodeType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:25
 */
public class ExpressBaseInjectableCode implements InjectableCode {
    private String content;
    private String expressionType;
    private String expressionValue;

    public ExpressBaseInjectableCode() {
    }

    public ExpressBaseInjectableCode(String content) {
        setContent(content);
    }

    @Override
    public CodeType getType() {
        return CodeType.valueOf(ExpressionCodeType.EXPRESSION);
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public String getExpressionType() {
        return expressionType;
    }

    public String getExpressionValue() {
        return expressionValue;
    }

    public void setContent(String content) {
        this.content = content;
        String[] parts = content.split(":");
        if (parts.length > 0) {
            this.expressionType = parts[0];
        }
        if (parts.length > 1) {
            this.expressionValue = parts[1];
        }
    }
}
