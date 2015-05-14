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
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.client.Client;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Tools;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


/**
 * Representing the message type mapping in ElasticSearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 */
public class Mapping {

    public static PutMappingRequest getPutMappingRequest(final Client client,
                                                         final String index,
                                                         final String analyzer,
                                                         boolean storeTimestampsAsDocValues) {
        final PutMappingRequestBuilder builder = client.admin().indices().preparePutMapping(index);
        builder.setType(Messages.TYPE);

        final Map<String, Object> mapping = ImmutableMap.of(
                "properties", partFieldProperties(analyzer, storeTimestampsAsDocValues),
                "dynamic_templates", partDefaultAllInDynamicTemplate(),
                // Compress source field
                "_source", enabledAndCompressed(),
                // Enable purging by TTL
                "_ttl", enabled());

        final Map<String, Map<String, Object>> completeMapping = ImmutableMap.of(Messages.TYPE, mapping);

        builder.setSource(completeMapping);
        return builder.request();
    }

    /*
     * Disable analyzing for every field by default.
     */
    private static List<Map<String, Map<String, Object>>> partDefaultAllInDynamicTemplate() {
        final Map<String, String> notAnalyzed = ImmutableMap.of("index", "not_analyzed");
        final Map<String, Object> defaultAll = ImmutableMap.of(
                // Match all
                "match", "*",
                // Analyze nothing by default
                "mapping", notAnalyzed);
        final Map<String, Map<String, Object>> template = ImmutableMap.of("store_generic", defaultAll);

        return ImmutableList.of(template);
    }

    /*
     * Enable analyzing for some fields again. Like for message and full_message.
     */
    private static Map<String, Map<String, ? extends Serializable>> partFieldProperties(String analyzer,
                                                                                        boolean storeTimestampsAsDocValues) {
        return ImmutableMap.of(
                "message", analyzedString(analyzer),
                "full_message", analyzedString(analyzer),
                // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
                // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
                "timestamp", typeTimeWithMillis(storeTimestampsAsDocValues),
                // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
                "source", analyzedString("analyzer_keyword"));
    }

    private static Map<String, String> analyzedString(String analyzer) {
        return ImmutableMap.of(
                "index", "analyzed",
                "type", "string",
                "analyzer", analyzer);
    }

    private static Map<String, Serializable> typeTimeWithMillis(boolean storeTimestampsAsDocValues) {
        final ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.builder();
        builder.put("type", "date")
                .put("format", Tools.ES_DATE_FORMAT);

        if (storeTimestampsAsDocValues) {
            builder.put("doc_values", true);
        }
        return builder.build();
    }

    private static Map<String, Boolean> enabled() {
        return ImmutableMap.of("enabled", true);
    }


    private static Map<String, Boolean> enabledAndCompressed() {
        return ImmutableMap.of(
                "enabled", true,
                "compress", true);
    }

}