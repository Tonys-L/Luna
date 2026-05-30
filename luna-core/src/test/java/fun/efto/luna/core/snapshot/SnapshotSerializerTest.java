package fun.efto.luna.core.snapshot;

import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotSerializer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:00
 */
public class SnapshotSerializerTest {

    static class CircularNode {
        String name;
        CircularNode next;
        
        public CircularNode(String name) {
            this.name = name;
        }
    }
    
    static class ComplexObj {
        int id = 100;
        String desc = "test \"escape\" \n and \t";
        List<String> items = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        
        public ComplexObj() {
            for (int i = 0; i < 25; i++) {
                items.add("item" + i);
            }
            map.put("key1", "val1");
            map.put("key2", 42);
        }
    }

    @Test
    public void testCircularReference() {
        CircularNode node1 = new CircularNode("Node1");
        CircularNode node2 = new CircularNode("Node2");
        node1.next = node2;
        node2.next = node1; // 循环引用
        
        String json = SnapshotSerializer.serialize(node1);
        System.out.println("Circular Reference JSON: " + json);
        assertTrue(json.contains("[CIRCULAR_REF]"));
    }
    
    @Test
    public void testDepthLimit() {
        CircularNode n1 = new CircularNode("1");
        CircularNode curr = n1;
        for (int i = 2; i <= 8; i++) {
            curr.next = new CircularNode(String.valueOf(i));
            curr = curr.next;
        }
        
        String json = SnapshotSerializer.serialize(n1);
        System.out.println("Depth Limit JSON: " + json);
        assertTrue(json.contains("[MAX_DEPTH]"));
    }
    
    @Test
    public void testCollectionTruncationAndEscape() {
        ComplexObj obj = new ComplexObj();
        String json = SnapshotSerializer.serialize(obj);
        System.out.println("Complex Object JSON: " + json);
        
        assertTrue(json.contains("more\"")); // 截断提示
        assertTrue(json.contains("\\\"escape\\\"")); // 引号转义
        assertTrue(json.contains("\\n")); // 换行转义
    }
}
