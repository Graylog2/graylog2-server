package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;

public abstract class AbstractMapping implements IndexMappingTemplate {
    protected Map.Entry<String, ImmutableMap<String, Object>> timestampField() {
        return Map.entry(FIELD_TIMESTAMP, map()
                .put("type", "date")
                .put("format", dateFormat())
                .build());
    }

    protected ImmutableMap.Builder<String, Object> map() {
        return ImmutableMap.builder();
    }

    protected ImmutableList.Builder<Object> list() {
        return ImmutableList.builder();
    }

    protected String dateFormat() {
        return ConstantsES7.ES_DATE_FORMAT;
    }
}
