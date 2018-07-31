package org.graylog2.indexer;

import static com.google.common.collect.ImmutableMap.of;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class IndexMapping6 extends IndexMapping {

    @Override
    public Map<String, Object> messageTemplate(final String template, final String analyzer, final int order) {
        final Map<String, Object> analyzerKeyword = ImmutableMap.of("analyzer_keyword", ImmutableMap.of(
            "tokenizer", "keyword",
            "filter", "lowercase"));
        final Map<String, Object> analysis = ImmutableMap.of("analyzer", analyzerKeyword);
        final Map<String, Object> settings = ImmutableMap.of("analysis", analysis);
        // mapping types are deprecated, 7.0 and later will only allow `_doc` here, so we start using that
        // see https://www.elastic.co/guide/en/elasticsearch/reference/6.3/removal-of-types.html#_schedule_for_removal_of_mapping_types
        final Map<String, Object> mappings = ImmutableMap.of("_doc", messageMapping(analyzer));

        return ImmutableMap.of(
            "template", template,
            "order", order,
            "settings", settings,
            "mappings", mappings
        );
    }

    protected List<Map<String, Map<String, Object>>> dynamicTemplate() {
        final Map<String, Object> defaultInternal = of(
            "match", "gl2_*",
            "mapping", notAnalyzedString());
        final Map<String, Map<String, Object>> templateInternal = of("internal_fields", defaultInternal);

        final Map<String, Object> defaultAll = of(
            // Match all
            "match", "*",
            // Analyze nothing by default
            "mapping", of("index", true),
            "type", "keyword");
        final Map<String, Map<String, Object>> templateAll = of("store_generic", defaultAll);

        return ImmutableList.of(templateInternal, templateAll);
    }

    @Override
    protected Map<String, Map<String, Object>> fieldProperties(String analyzer) {
        return null;
    }

    @Override
    protected Map<String, Object> notAnalyzedString() {
        return null;
    }
}
