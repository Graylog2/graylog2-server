package org.graylog.storage.elasticsearch6;

import io.searchbox.core.Index;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.plugin.Message;

import java.util.Map;

public class IndexingHelper {
    public Index prepareIndexRequest(String index, Map<String, Object> source, String id) {
        source.remove(Message.FIELD_ID);

        return new Index.Builder(source)
                .index(index)
                .type(IndexMapping.TYPE_MESSAGE)
                .id(id)
                .build();
    }
}
