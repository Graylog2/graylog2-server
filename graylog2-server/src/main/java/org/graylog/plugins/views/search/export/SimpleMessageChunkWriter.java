package org.graylog.plugins.views.search.export;

import javax.ws.rs.ext.MessageBodyWriter;
import java.lang.reflect.Type;

public abstract class SimpleMessageChunkWriter implements MessageBodyWriter<SimpleMessageChunk> {
    protected boolean typesMatch(Class<?> type, Type genericType) {
        return SimpleMessageChunk.class.equals(type) || isAutoValueType(type, genericType);
    }

    private boolean isAutoValueType(Class<?> type, Type genericType) {
        return AutoValue_SimpleMessageChunk.class.equals(type) && SimpleMessageChunk.class.equals(genericType);
    }
}
