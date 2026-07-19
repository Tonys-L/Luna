package fun.efto.luna.core.probe.invocation.model;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/05 20:00
 */
public interface InvocationContextCarrier {

    ActiveInvocation current();

    void push(ActiveInvocation invocation);

    ActiveInvocation pop();
}
