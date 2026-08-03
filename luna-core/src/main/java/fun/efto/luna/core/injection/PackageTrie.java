package fun.efto.luna.core.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class PackageTrie {
    private static class Node {
        final Map<String, Node> children = new ConcurrentHashMap<>();
        final List<InjectionPoint> points = new CopyOnWriteArrayList<>();
    }
    
    private final Node root = new Node();
    
    public void insert(String prefix, InjectionPoint point) {
        if (prefix == null || prefix.isEmpty()) {
            root.points.add(point);
            return;
        }
        
        String[] parts = prefix.split("\\.");
        Node current = root;
        for (String part : parts) {
            current = current.children.computeIfAbsent(part, k -> new Node());
        }
        current.points.add(point);
    }
    
    public List<InjectionPoint> findPrefixes(String className) {
        List<InjectionPoint> results = new ArrayList<>();
        if (className == null || className.isEmpty()) {
            return results;
        }
        
        String[] parts = className.split("\\.");
        Node current = root;
        
        results.addAll(current.points);
        
        for (String part : parts) {
            current = current.children.get(part);
            if (current == null) {
                break;
            }
            results.addAll(current.points);
        }
        return results;
    }
    
    public void removeById(String id) {
        removeByIdRecursive(root, id);
    }
    
    private void removeByIdRecursive(Node node, String id) {
        node.points.removeIf(p -> p.getId().equals(id));
        for (Node child : node.children.values()) {
            removeByIdRecursive(child, id);
        }
    }

    public boolean containsById(String id) {
        return containsByIdRecursive(root, id);
    }

    private boolean containsByIdRecursive(Node node, String id) {
        for (InjectionPoint p : node.points) {
            if (p.getId().equals(id)) return true;
        }
        for (Node child : node.children.values()) {
            if (containsByIdRecursive(child, id)) return true;
        }
        return false;
    }
}
