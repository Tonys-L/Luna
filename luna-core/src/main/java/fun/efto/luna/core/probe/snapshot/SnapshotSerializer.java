package fun.efto.luna.core.probe.snapshot;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import fun.efto.luna.core.probe.BootstrapClassRegistry;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:00
 */
public class SnapshotSerializer {

    static {
        BootstrapClassRegistry.register(SnapshotSerializer.class.getName());
    }

    private static final int MAX_DEPTH = 5;
    private static final int MAX_COLLECTION_SIZE = 20;
    private static final int MAX_STRING_LENGTH = 200;

    public static String serialize(Object obj) {
        StringBuilder sb = new StringBuilder();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        serializeInternal(obj, sb, visited, 0);
        return sb.toString();
    }

    private static void serializeInternal(Object obj, StringBuilder sb, IdentityHashMap<Object, Boolean> visited, int depth) {
        if (obj == null) {
            sb.append("null");
            return;
        }

        Class<?> clazz = obj.getClass();
        if (isWrapperOrPrimitive(clazz)) {
            sb.append(obj);
            return;
        }

        if (obj instanceof String) {
            sb.append("\"").append(escapeAndTruncate((String) obj)).append("\"");
            return;
        }

        if (depth >= MAX_DEPTH) {
            sb.append("\"[MAX_DEPTH] ").append(clazz.getSimpleName()).append("@").append(Integer.toHexString(System.identityHashCode(obj))).append("\"");
            return;
        }

        if (visited.containsKey(obj)) {
            sb.append("\"[CIRCULAR_REF]\"");
            return;
        }
        visited.put(obj, Boolean.TRUE);

        try {
            if (clazz.isArray()) {
                serializeArray(obj, sb, visited, depth);
            } else if (obj instanceof Collection) {
                serializeCollection((Collection<?>) obj, sb, visited, depth);
            } else if (obj instanceof Map) {
                serializeMap((Map<?, ?>) obj, sb, visited, depth);
            } else {
                serializeObject(obj, clazz, sb, visited, depth);
            }
        } catch (Throwable t) {
            sb.append("\"[ERROR_SERIALIZING: ").append(t.getClass().getSimpleName()).append("]\"");
        }
    }

    private static void serializeArray(Object array, StringBuilder sb, IdentityHashMap<Object, Boolean> visited, int depth) {
        sb.append("[");
        int length = Array.getLength(array);
        int limit = Math.min(length, MAX_COLLECTION_SIZE);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(",");
            serializeInternal(Array.get(array, i), sb, visited, depth + 1);
        }
        if (length > MAX_COLLECTION_SIZE) {
            sb.append(",\"...").append(length - MAX_COLLECTION_SIZE).append(" more\"]");
        }
        sb.append("]");
    }

    private static void serializeCollection(Collection<?> collection, StringBuilder sb, IdentityHashMap<Object, Boolean> visited, int depth) {
        sb.append("[");
        int count = 0;
        for (Object item : collection) {
            if (count > 0) sb.append(",");
            if (count >= MAX_COLLECTION_SIZE) {
                sb.append("\"...").append(collection.size() - MAX_COLLECTION_SIZE).append(" more\"");
                break;
            }
            serializeInternal(item, sb, visited, depth + 1);
            count++;
        }
        sb.append("]");
    }

    private static void serializeMap(Map<?, ?> map, StringBuilder sb, IdentityHashMap<Object, Boolean> visited, int depth) {
        sb.append("{");
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count > 0) sb.append(",");
            if (count >= MAX_COLLECTION_SIZE) {
                sb.append("\"...\":").append("\"").append(map.size() - MAX_COLLECTION_SIZE).append(" more\"");
                break;
            }
            String keyStr = String.valueOf(entry.getKey());
            sb.append("\"").append(escapeAndTruncate(keyStr)).append("\":");
            serializeInternal(entry.getValue(), sb, visited, depth + 1);
            count++;
        }
        sb.append("}");
    }

    private static void serializeObject(Object obj, Class<?> clazz, StringBuilder sb, IdentityHashMap<Object, Boolean> visited, int depth) {
        if (clazz.getName().startsWith("java.") && !clazz.getName().startsWith("java.util.")) {
            sb.append("\"").append(escapeAndTruncate(obj.toString())).append("\"");
            return;
        }

        sb.append("{");
        Field[] fields = clazz.getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("\"").append(field.getName()).append("\":");

            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                serializeInternal(value, sb, visited, depth + 1);
            } catch (Throwable t) {
                sb.append("\"[ERROR:").append(t.getClass().getSimpleName()).append("]\"");
            }
        }
        sb.append("}");
    }

    private static boolean isWrapperOrPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == Integer.class || clazz == Long.class ||
               clazz == Boolean.class || clazz == Double.class ||
               clazz == Float.class || clazz == Byte.class ||
               clazz == Short.class || clazz == Character.class;
    }

    private static String escapeAndTruncate(String str) {
        if (str == null) return "null";
        if (str.length() > MAX_STRING_LENGTH) {
            str = str.substring(0, MAX_STRING_LENGTH) + "...";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
