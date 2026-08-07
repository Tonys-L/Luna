package fun.efto.luna.core.analysis.decompile;

import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.OutputSinkFactory.SinkClass;
import org.benf.cfr.reader.api.OutputSinkFactory.SinkType;
import org.benf.cfr.reader.api.SinkReturns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/20 20:44
 */
public class SimpleOutputSinkFactory implements OutputSinkFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOutputSinkFactory.class);
    private final StringBuilder decompileBuilder = new StringBuilder();
    private Throwable exception;
    private final NavigableMap<Integer, Integer> lineMapping = new TreeMap<>();

    @Override
    public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
        switch (sinkType) {
            case JAVA:
                return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.DECOMPILED_MULTIVER);
            case LINENUMBER:
                return Arrays.asList(SinkClass.LINE_NUMBER_MAPPING);
            case EXCEPTION:
                return Arrays.asList(SinkClass.EXCEPTION_MESSAGE);
            default:
                return Arrays.asList(SinkClass.STRING);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
        switch (sinkType) {
            case JAVA:
                return v -> {
                    decompileBuilder.append(v.toString());
                };
            case EXCEPTION:
                return v -> {
                    if (v instanceof SinkReturns.ExceptionMessage) {
                        // CFR 0.152 的 SinkExceptionDumper 传入此类型，含真正异常
                        SinkReturns.ExceptionMessage em = (SinkReturns.ExceptionMessage) v;
                        Exception thrown = em.getThrownException();
                        String path = em.getPath();
                        String message = em.getMessage();
                        String detail = (path != null ? path : "") + (message != null ? ": " + message : "");
                        if (thrown != null) {
                            exception = new RuntimeException(detail, thrown);
                        } else {
                            exception = new RuntimeException(detail);
                        }
                    } else if (v instanceof Exception) {
                        exception = (Exception) v;
                    } else if (v instanceof CharSequence) {
                        exception = new RuntimeException(v.toString());
                    } else {
                        exception = new RuntimeException("CFR reported: " + String.valueOf(v));
                    }
                };
            case LINENUMBER:
                return v -> {
                    try {
                        if (v instanceof SinkReturns.LineNumberMapping) {
                            SinkReturns.LineNumberMapping mapping = (SinkReturns.LineNumberMapping) v;
                            NavigableMap<Integer, Integer> classFileMappings = mapping.getClassFileMappings();
                            NavigableMap<Integer, Integer> mappings = mapping.getMappings();
                            if (classFileMappings != null && mappings != null) {
                                for (java.util.Map.Entry<Integer, Integer> entry : mappings.entrySet()) {
                                    Integer srcLineNumber = classFileMappings.get(entry.getKey());
                                    if (srcLineNumber != null) {
                                        lineMapping.put(entry.getValue(), srcLineNumber);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Failed to process line number mapping: {}", e.getMessage());
                    }
                };
            default:
                return v -> {};
        }
    }

    public String getDecompile() {
        return decompileBuilder.toString();
    }

    public Throwable getException() {
        return exception;
    }

    public NavigableMap<Integer, Integer> getLineMapping() {
        return lineMapping;
    }
}
