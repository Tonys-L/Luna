package fun.efto.luna.core.plugin.lifecycle;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */

// TODO: ReadyGate is part of the plugin-architecture-v4 "startup barrier" design.
//  Planned enhancements (see docs/design/luna-plugin-architecture-v4.md §6.1):
//  1. Add await(long timeout, TimeUnit unit) to block until ready, enabling
//     RuleClassFileTransformer to wait for all plugins before transforming.
//  2. Add compensating retransform trigger: once markReady() is called,
//     retransform classes that were loaded before plugins were ready.
//  3. Currently only used as a simple volatile flag; the full barrier semantics
//     are not yet wired into the transformer pipeline.
public class ReadyGate {

    private volatile boolean ready = false;

    public void markReady() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }
}
