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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Representing the message type mapping in Elasticsearch 5.x. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/5.4/mapping.html">Elasticsearch Reference / Mapping</a>
 */
public class IndexMapping5 extends IndexMapping {
    @Override
    protected Map<String, Map<String, Object>> fieldProperties(String analyzer) {
        return ImmutableMap.of(
                "message", analyzedString(analyzer, false),
                "full_message", analyzedString(analyzer, false),
                // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
                // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
                "timestamp", typeTimeWithMillis(),
                // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
                "source", analyzedString("analyzer_keyword", true),
                "streams", notAnalyzedString());
    }

    @Override
    protected Map<String, Object> notAnalyzedString() {
        return ImmutableMap.of("type", "keyword");
    }

    private Map<String, Object> analyzedString(String analyzer, boolean fieldData) {
        return ImmutableMap.of(
                "type", "text",
                "analyzer", analyzer,
                "fielddata", fieldData);
    }
}
