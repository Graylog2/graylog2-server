package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Message;

import java.util.Map;

public class IndexMapping7 extends IndexMapping {
    public Map<String, Object> messageTemplate(final String template, final String analyzer, final int order) {
        final Map<String, Object> analyzerKeyword = ImmutableMap.of("analyzer_keyword", ImmutableMap.of(
                "tokenizer", "keyword",
                "filter", "lowercase"));
        final Map<String, Object> analysis = ImmutableMap.of("analyzer", analyzerKeyword);
        final Map<String, Object> settings = ImmutableMap.of("analysis", analysis);
        final Map<String, Object> mappings = messageMapping(analyzer);

        return ImmutableMap.of(
                "template", template,
                "order", order,
                "settings", settings,
                "mappings", mappings
        );
    }

    @Override
    protected Map<String, Map<String, Object>> fieldProperties(String analyzer) {
        return ImmutableMap.<String, Map<String, Object>>builder()
                .put("message", analyzedString(analyzer, false))
                .put("full_message", analyzedString(analyzer, false))
                // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
                // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
                .put("timestamp", typeTimeWithMillis())
                .put(Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, typeLong())
                .put(Message.FIELD_GL2_RECEIVE_TIMESTAMP, typeTimeWithMillis())
                .put(Message.FIELD_GL2_PROCESSING_TIMESTAMP, typeTimeWithMillis())
                // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
                .put("source", analyzedString("analyzer_keyword", true))
                .put("streams", notAnalyzedString())
                .build();
    }

    private Map<String, Object> analyzedString(String analyzer, boolean fieldData) {
        return ImmutableMap.of(
                "type", "text",
                "analyzer", analyzer,
                "fielddata", fieldData);
    }

    @Override
    protected Map<String, Object> notAnalyzedString() {
        return ImmutableMap.of("type", "keyword");
    }
}
