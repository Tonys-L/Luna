package fun.efto.luna.core.decompile;

import org.benf.cfr.reader.api.OutputSinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/20 20:44
 */
public class SimpleOutputSinkFactory implements OutputSinkFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOutputSinkFactory.class);
    private String decompile;
    private Throwable exception;

    @Override
    public List<OutputSinkFactory.SinkClass> getSupportedSinks(OutputSinkFactory.SinkType sinkType, Collection<OutputSinkFactory.SinkClass> collection) {
        return Arrays.asList(SinkClass.values());
    }

    @Override
    public <T> OutputSinkFactory.Sink<T> getSink(OutputSinkFactory.SinkType sinkType, OutputSinkFactory.SinkClass sinkClass) {
        switch (sinkType) {
            case JAVA:
                return v -> decompile = v.toString();
            case EXCEPTION:
                return v -> {
                    if (v instanceof Exception) {
                        exception = (Exception) v;
                    } else {
                        LOGGER.error("Unexpected error:{}", v);
                    }
                };
            default:
                return v -> LOGGER.error("其让类型先忽略，type:{},v:{}", sinkType, v);
        }
    }

    public String getDecompile() {
        return decompile;
    }

    public Throwable getException() {
        return exception;
    }
}
