package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.target.BaseTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.InjectionTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InjectionRegistryTest {

    private InjectionRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new DefaultInjectionRegistry();
    }

    private InjectionPoint createPoint(String id, String targetClass) {
        InjectionTarget target = new BaseTarget(InjectionLocation.of("mock", "mock"), targetClass) {};
        PersistentInjection source = new PersistentInjection();
        source.setId(id);
        return new InjectionPoint(id, target, null, "mock", "mock", source);
    }

    @Test
    public void testExactMatch() {
        InjectionPoint p1 = createPoint("p1", "fun.efto.luna.Service");
        registry.register(p1);

        List<InjectionPoint> results = registry.getActivePointsForClass("fun.efto.luna.Service");
        assertEquals(1, results.size());
        assertEquals("p1", results.get(0).getId());

        assertTrue(registry.getActivePointsForClass("fun.efto.luna.Service2").isEmpty());
    }

    @Test
    public void testPrefixTrieMatch() {
        InjectionPoint p1 = createPoint("p1", "fun.efto.luna.*");
        registry.register(p1);

        List<InjectionPoint> results = registry.getActivePointsForClass("fun.efto.luna.agent.Agent");
        assertEquals(1, results.size());
        assertEquals("p1", results.get(0).getId());

        List<InjectionPoint> results2 = registry.getActivePointsForClass("fun.efto.other.Service");
        assertTrue(results2.isEmpty());
    }

    @Test
    public void testMultipleWildcardsInTrie() {
        registry.register(createPoint("p1", "fun.efto.luna.*"));
        registry.register(createPoint("p2", "fun.efto.*"));
        registry.register(createPoint("p3", "fun.efto.luna.agent.*"));
        registry.register(createPoint("p4", "com.example.*"));

        List<InjectionPoint> results = registry.getActivePointsForClass("fun.efto.luna.agent.Agent");
        assertEquals(3, results.size(), "Should match fun.efto.*, fun.efto.luna.*, and fun.efto.luna.agent.*");
        
        // Ensure they all match
        assertTrue(results.stream().anyMatch(p -> p.getId().equals("p1")));
        assertTrue(results.stream().anyMatch(p -> p.getId().equals("p2")));
        assertTrue(results.stream().anyMatch(p -> p.getId().equals("p3")));
    }

    @Test
    public void testComplexRegexFallback() {
        InjectionPoint p1 = createPoint("p1", ".*ServiceImpl");
        registry.register(p1);

        List<InjectionPoint> results = registry.getActivePointsForClass("fun.efto.luna.UserServiceImpl");
        assertEquals(1, results.size());
        assertEquals("p1", results.get(0).getId());

        List<InjectionPoint> results2 = registry.getActivePointsForClass("fun.efto.luna.UserService");
        assertTrue(results2.isEmpty());
    }
    
    @Test
    public void testUnregister() {
        registry.register(createPoint("p1", "fun.efto.luna.*"));
        assertEquals(1, registry.getActivePointsForClass("fun.efto.luna.Service").size());
        
        registry.unregister("p1");
        assertTrue(registry.getActivePointsForClass("fun.efto.luna.Service").isEmpty());
    }

    @Test
    public void testPerformance() {
        // Register 1000 prefix points
        for (int i = 0; i < 1000; i++) {
            registry.register(createPoint("p" + i, "fun.efto.luna.pkg" + i + ".*"));
        }
        
        // Register a few regex points
        registry.register(createPoint("regex1", ".*Service.*"));
        registry.register(createPoint("regex2", ".*Controller.*"));

        // Register exact point
        registry.register(createPoint("exact", "fun.efto.luna.pkg500.MyService"));

        // Warmup
        for (int i = 0; i < 10000; i++) {
            registry.getActivePointsForClass("fun.efto.luna.pkg500.MyService");
        }

        long start = System.nanoTime();
        int queries = 10000;
        for (int i = 0; i < queries; i++) {
            registry.getActivePointsForClass("fun.efto.luna.pkg500.MyService");
        }
        long end = System.nanoTime();
        
        double avgMs = (end - start) / 1_000_000.0 / queries;
        System.out.println("Average query time: " + avgMs + " ms");
        assertTrue(avgMs < 0.1, "Query time " + avgMs + "ms exceeds 0.1ms limit");
    }
}
