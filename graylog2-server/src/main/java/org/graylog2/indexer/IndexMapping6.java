package org.graylog2.indexer;

import static com.google.common.collect.ImmutableMap.of;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class IndexMapping6 extends IndexMapping {

    @Override
    public Map<String, Object> messageTemplate(final String template, final String analyzer,
        final int order) {
        final Map<String, Object> analyzerKeyword = ImmutableMap
            .of("analyzer_keyword", ImmutableMap.of(
                "tokenizer", "keyword",
                "filter", "lowercase"));
        final Map<String, Object> analysis = ImmutableMap.of("analyzer", analyzerKeyword);
        final Map<String, Object> settings = ImmutableMap.of("analysis", analysis);
        // mapping types are deprecated, 7.0 and later will only allow `_doc` here, so we start using that
        // see https://www.elastic.co/guide/en/elasticsearch/reference/6.3/removal-of-types.html#_schedule_for_removal_of_mapping_types
        final Map<String, Object> mappings = ImmutableMap.of(IndexMapping.TYPE_MESSAGE, messageMapping(analyzer));

        return ImmutableMap.of(
            "template", template,
            "order", order,
            "settings", settings,
            "mappings", mappings
        );
    }

    protected List<Map<String, Map<String, Object>>> dynamicTemplate() {
        final Map<String, Map<String, Object>> templateInternal =
            of("internal_fields", of(
                "match", "gl2_*",
                "mapping", notAnalyzedString()));

        final Map<String, Object> dynamicStrings = of(
            "match_mapping_type", "string",
            "mapping", notAnalyzedString()
        );
        final Map<String, Map<String, Object>> templateAll = of("store_generic", dynamicStrings);

        return ImmutableList.of(templateInternal, templateAll);
    }

    @Override
    protected Map<String, Map<String, Object>> fieldProperties(String analyzer) {
        return of(
            "message", analyzedString(analyzer, false),
            "full_message", analyzedString(analyzer, false),
            // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
            // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
            "timestamp", typeTimeWithMillis(),
            // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
            "source", analyzedString("analyzer_keyword", true),
            "streams", notAnalyzedString());
    }

    private Map<String, Object> analyzedString(String analyzer, boolean fieldData) {
        return of(
            "type", "text",
            "analyzer", analyzer,
            "fielddata", fieldData);
    }

    @Override
    protected Map<String, Object> notAnalyzedString() {
        return of("type", "keyword");
    }
}
