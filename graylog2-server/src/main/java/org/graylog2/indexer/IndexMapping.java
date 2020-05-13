/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;

import java.util.List;
import java.util.Map;

/**
 * Representing the message type mapping in Elasticsearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 */
public abstract class IndexMapping implements IndexMappingTemplate {
    public static final String TYPE_MESSAGE = "message";

    @Override
    public Map<String, Object> toTemplate(IndexSetConfig indexSetConfig, String indexPattern, int order) {
        return messageTemplate(indexPattern, indexSetConfig.indexAnalyzer(), order);
    }

    public Map<String, Object> messageTemplate(final String template, final String analyzer, final int order) {
        final Map<String, Object> analyzerKeyword = ImmutableMap.of("analyzer_keyword", ImmutableMap.of(
                "tokenizer", "keyword",
                "filter", "lowercase"));
        final Map<String, Object> analysis = ImmutableMap.of("analyzer", analyzerKeyword);
        final Map<String, Object> settings = ImmutableMap.of("analysis", analysis);
        final Map<String, Object> mappings = ImmutableMap.of(TYPE_MESSAGE, messageMapping(analyzer));

        return ImmutableMap.of(
                "template", template,
                "order", order,
                "settings", settings,
                "mappings", mappings
        );
    }

    private Map<String, Object> messageMapping(final String analyzer) {
        return ImmutableMap.of(
                "properties", fieldProperties(analyzer),
                "dynamic_templates", dynamicTemplate(),
                "_source", enabled());
    }

    private Map<String, Map<String, Object>> internalFieldsMapping() {
        return ImmutableMap.of("internal_fields",
                ImmutableMap.of(
                        "match", "gl2_*",
                        "match_mapping_type", "string",
                        "mapping", notAnalyzedString())
        );
    }

    private List<Map<String, Map<String, Object>>> dynamicTemplate() {
        final Map<String, Map<String, Object>> templateInternal = internalFieldsMapping();

        final Map<String, Map<String, Object>> templateAll = ImmutableMap.of("store_generic", dynamicStrings());

        return ImmutableList.of(templateInternal, templateAll);
    }

    protected Map<String, Object> dynamicStrings() {
        return ImmutableMap.of(
                    // Match all
                    "match", "*",
                    // Analyze nothing by default
                    "mapping", ImmutableMap.of("index", "not_analyzed"));
    }

    private Map<String, Map<String, Object>> fieldProperties(String analyzer) {
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

    protected Map<String, Object> notAnalyzedString() {
        return ImmutableMap.of("type", "keyword");
    }
    protected Map<String, Object> analyzedString(String analyzer, boolean fieldData) {
        return ImmutableMap.of(
                "type", "text",
                "analyzer", analyzer,
                "fielddata", fieldData);
    }

    private Map<String, Object> typeTimeWithMillis() {
        return ImmutableMap.of(
                "type", "date",
                "format", Tools.ES_DATE_FORMAT);
    }

    private Map<String, Object> typeLong() {
        return ImmutableMap.of("type", "long");
    }

    private Map<String, Boolean> enabled() {
        return ImmutableMap.of("enabled", true);
    }
}
