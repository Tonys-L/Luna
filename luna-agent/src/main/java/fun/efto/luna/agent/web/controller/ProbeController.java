package fun.efto.luna.agent.web.controller;

import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.RequestMapping;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.CodeEngineRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 02:30
 */
@Controller
@RequestMapping("/probes")
public class ProbeController {

    private final ProbeHandlerRegistry probeHandlerRegistry;
    private final CodeEngineRegistry codeEngineRegistry;

    public ProbeController(ProbeHandlerRegistry probeHandlerRegistry, CodeEngineRegistry codeEngineRegistry) {
        this.probeHandlerRegistry = probeHandlerRegistry;
        this.codeEngineRegistry = codeEngineRegistry;
    }

    @GetMapping
    public ApiResult listProbes() {
        List<Map<String, Object>> probes = new ArrayList<>();
        for (String probeType : probeHandlerRegistry.getRegisteredTypes()) {
            ProbeHandler handler = probeHandlerRegistry.get(probeType).orElse(null);
            if (handler != null) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("probeType", handler.getProbeType());
                entry.put("usesCode", handler.usesCode());
                entry.put("supportedInjectionLocations", new ArrayList<>(handler.supportedInjectionLocations()));
                probes.add(entry);
            }
        }
        return ApiResult.ok(probes);
    }

    @GetMapping("/engines")
    public ApiResult listEngines() {
        List<Map<String, Object>> engines = new ArrayList<>();
        for (String codeType : codeEngineRegistry.getRegisteredTypes()) {
            CodeEngine engine = codeEngineRegistry.get(codeType).orElse(null);
            if (engine != null) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("codeType", engine.getCodeType());
                engines.add(entry);
            }
        }
        return ApiResult.ok(engines);
    }
}
