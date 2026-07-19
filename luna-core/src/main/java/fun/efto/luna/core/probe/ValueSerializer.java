package fun.efto.luna.core.probe;

import java.util.Collection;
import java.util.Map;

/**
 * Shared value serializer for probe output.
 * Handles primitives, strings, collections, maps, and arbitrary objects
 * with defensive truncation to prevent excessive output.
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/18 22:45
 */
public final class ValueSerializer {

    public static final String INTERNAL_NAME = ValueSerializer.class.getName().replace('.', '/');

    private static final int MAX_STRING_LENGTH = 256;
    private static final int MAX_COLLECTION_ELEMENTS = 5;
    private static final int MAX_FIELD_DEPTH = 2;

    private ValueSerializer() {}

    public static String serialize(Object value) {
        return serialize(value, 0);
    }

    private static String serialize(Object value, int depth) {
        if (value == null) return "null";
        try {
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof String) {
                String s = (String) value;
                if (s.length() > MAX_STRING_LENGTH) {
                    return "\"" + s.substring(0, MAX_STRING_LENGTH) + "...\"";
                }
                return "\"" + s + "\"";
            }
            if (value instanceof Character) {
                return "'" + value + "'";
            }
            if (value instanceof Collection) {
                Collection<?> coll = (Collection<?>) value;
                StringBuilder sb = new StringBuilder();
                sb.append(coll.getClass().getSimpleName()).append("(size=").append(coll.size()).append(")[");
                int count = 0;
                for (Object elem : coll) {
                    if (count >= MAX_COLLECTION_ELEMENTS) {
                        sb.append(", ...");
                        break;
                    }
                    if (count > 0) sb.append(", ");
                    sb.append(serialize(elem, depth + 1));
                    count++;
                }
                sb.append("]");
                return sb.toString();
            }
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                StringBuilder sb = new StringBuilder();
                sb.append(map.getClass().getSimpleName()).append("(size=").append(map.size()).append("){");
                int count = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (count >= MAX_COLLECTION_ELEMENTS) {
                        sb.append(", ...");
                        break;
                    }
                    if (count > 0) sb.append(", ");
                    sb.append(serialize(entry.getKey(), depth + 1))
                      .append("=")
                      .append(serialize(entry.getValue(), depth + 1));
                    count++;
                }
                sb.append("}");
                return sb.toString();
            }

            // For objects: if they have a custom toString(), use it;
            // otherwise use reflection-based field dump (limited depth)
            String str = value.toString();
            boolean defaultToString = str.equals(value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value)));

            if (defaultToString && depth < MAX_FIELD_DEPTH) {
                return serializeFields(value, depth);
            }

            if (str.length() > MAX_STRING_LENGTH) {
                return value.getClass().getSimpleName() + "{" + str.substring(0, MAX_STRING_LENGTH) + "...}";
            }
            return value.getClass().getSimpleName() + "{" + str + "}";
        } catch (Throwable t) {
            return "[serialization error: " + value.getClass().getName() + "]";
        }
    }

    /**
     * Use reflection to dump an object's declared fields when toString() is not overridden.
     */
    private static String serializeFields(Object value, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(value.getClass().getSimpleName()).append("{");

        Class<?> clazz = value.getClass();
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        boolean first = true;
        int count = 0;

        for (java.lang.reflect.Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (count >= MAX_COLLECTION_ELEMENTS) {
                sb.append(", ...");
                break;
            }
            if (!first) sb.append(", ");
            first = false;

            field.setAccessible(true);
            try {
                Object fieldValue = field.get(value);
                sb.append(field.getName()).append("=").append(serialize(fieldValue, depth + 1));
            } catch (Throwable t) {
                sb.append(field.getName()).append("=<access error>");
            }
            count++;
        }

        sb.append("}");
        return sb.toString();
    }
}
