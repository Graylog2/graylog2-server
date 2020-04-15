package org.graylog.plugins.views.search.export;

import org.graylog.plugins.views.search.searchtypes.MessageList;

public interface ChunkDecorator {
    SimpleMessageChunk decorate(SimpleMessageChunk chunk, MessageList ml);
}
