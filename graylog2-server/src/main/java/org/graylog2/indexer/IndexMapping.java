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

    public Map<String, Object> messageTemplate(final String template, final String analyzer) {
        return messageTemplate(template, analyzer, -1);
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

    protected Map<String, Object> messageMapping(final String analyzer) {
        return ImmutableMap.of(
                "properties", fieldProperties(analyzer),
                "dynamic_templates", dynamicTemplate(),
                "_source", enabled());
    }

    protected List<Map<String, Map<String, Object>>> dynamicTemplate() {
        final Map<String, Object> defaultInternal = ImmutableMap.of(
                "match", "gl2_*",
                "match_mapping_type", "string",
                "mapping", notAnalyzedString());
        final Map<String, Map<String, Object>> templateInternal = ImmutableMap.of("internal_fields", defaultInternal);

        final Map<String, Object> defaultAll = ImmutableMap.of(
                // Match all
                "match", "*",
                // Analyze nothing by default
                "mapping", ImmutableMap.of("index", "not_analyzed"));
        final Map<String, Map<String, Object>> templateAll = ImmutableMap.of("store_generic", defaultAll);

        return ImmutableList.of(templateInternal, templateAll);
    }

    protected abstract Map<String, Map<String, Object>> fieldProperties(String analyzer);

    protected abstract Map<String, Object> notAnalyzedString();

    protected Map<String, Object> typeTimeWithMillis() {
        return ImmutableMap.of(
                "type", "date",
                "format", Tools.ES_DATE_FORMAT);
    }

    protected Map<String, Object> typeLong() {
        return ImmutableMap.of("type", "long");
    }

    protected Map<String, Boolean> enabled() {
        return ImmutableMap.of("enabled", true);
    }
}
