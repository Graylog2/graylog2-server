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

import java.util.Map;

public abstract class EventsIndexMapping implements IndexMappingTemplate {
    @Override
    public Map<String, Object> toTemplate(IndexSetConfig indexSetConfig, String indexPattern, int order) {
        final String indexPatternsField = "index_patterns";
        final String indexRefreshInterval = "1s"; // TODO: Index refresh interval must be configurable

        return map()
                .put(indexPatternsField, indexPattern)
                .put("order", order)
                .put("settings", map()
                        .put("index.refresh_interval", indexRefreshInterval)
                        .build())
                .put("mappings", buildMappings())
                .build();
    }

    protected ImmutableMap<String, Object> buildMappings() {
        return map()
                .put("_source", map()
                        .put("enabled", true)
                        .build())
                .put("dynamic", false)
                .put("dynamic_templates", list()
                        .add(map()
                                .put("fields", map()
                                        .put("path_match", "fields.*")
                                        .put("mapping", map()
                                                .put("type", "keyword")
                                                .put("doc_values", true)
                                                .put("index", true)
                                                .build())
                                        .build())
                                .build())
                        /* TODO: Enable the typed fields once we decided if that's the way to go
                        .add(map()
                                .put("fields-typed-long", map()
                                        .put("path_match", "fields_typed.long.*")
                                        .put("mapping", map()
                                                .put("type", "long")
                                                .put("doc_values", true)
                                                .put("index", true)
                                                .build())
                                        .build())
                                .build())
                        .add(map()
                                .put("fields-typed-double", map()
                                        .put("path_match", "fields_typed.double.*")
                                        .put("mapping", map()
                                                .put("type", "double")
                                                .put("doc_values", true)
                                                .put("index", true)
                                                .build())
                                        .build())
                                .build())
                        .add(map()
                                .put("fields-typed-boolean", map()
                                        .put("path_match", "fields_typed.boolean.*")
                                        .put("mapping", map()
                                                .put("type", "boolean")
                                                .put("doc_values", true)
                                                .put("index", true)
                                                .build())
                                        .build())
                                .build())
                        .add(map()
                                .put("fields-typed-ip", map()
                                        .put("path_match", "fields_typed.ip.*")
                                        .put("mapping", map()
                                                .put("type", "ip")
                                                .put("doc_values", true)
                                                .put("index", true)
                                                .build())
                                        .build())
                                .build())
                         */
                        .build())
                .put("properties", fieldProperties())
                .build();
    }

    protected ImmutableMap<String, Object> fieldProperties() {
        return map()
                .put("id", map()
                        .put("type", "keyword")
                        .build())
                .put("event_definition_type", map()
                        .put("type", "keyword")
                        .build())
                .put("event_definition_id", map()
                        .put("type", "keyword")
                        .build())
                .put("origin_context", map()
                        .put("type", "keyword")
                        .build())
                .put("timestamp", map()
                        .put("type", "date")
                        // Use the same format we use for the "message" mapping to make sure we
                        // can use the search.
                        .put("format", Tools.ES_DATE_FORMAT)
                        .build())
                .put("timestamp_processing", map()
                        .put("type", "date")
                        // Use the same format we use for the "message" mapping to make sure we
                        // can use the search.
                        .put("format", Tools.ES_DATE_FORMAT)
                        .build())
                .put("timerange_start", map()
                        .put("type", "date")
                        // Use the same format we use for the "message" mapping to make sure we
                        // can use the search.
                        .put("format", Tools.ES_DATE_FORMAT)
                        .build())
                .put("timerange_end", map()
                        .put("type", "date")
                        // Use the same format we use for the "message" mapping to make sure we
                        // can use the search.
                        .put("format", Tools.ES_DATE_FORMAT)
                        .build())
                .put("streams", map()
                        .put("type", "keyword")
                        .build())
                .put("source_streams", map()
                        .put("type", "keyword")
                        .build())
                .put("message", map()
                        .put("type", "text")
                        .put("analyzer", "standard")
                        .put("norms", false)
                        .put("fields", map()
                                .put("keyword", map()
                                        .put("type", "keyword")
                                        .build())
                                .build())
                        .build())
                .put("source", map()
                        .put("type", "keyword")
                        .build())
                .put("key", map()
                        .put("type", "keyword")
                        .build())
                .put("key_tuple", map()
                        .put("type", "keyword")
                        .build())
                .put("priority", map()
                        .put("type", "long")
                        .build())
                .put("alert", map()
                        .put("type", "boolean")
                        .build())
                .put("fields", map()
                        .put("type", "object")
                        .put("dynamic", true)
                        .build())
                /* TODO: Enable the typed fields once we decided if that's the way to go
                .put("fields_typed", map()
                        .put("type", "object")
                        .put("properties", map()
                                .put("long", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .put("double", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .put("boolean", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .put("ip", map()
                                        .put("type", "object")
                                        .put("dynamic", true)
                                        .build())
                                .build())
                        .build())
                 */
                .put("triggered_jobs", map()
                        .put("type", "keyword")
                        .build())
                .build();
    }

    protected ImmutableMap.Builder<String, Object> map() {
        return ImmutableMap.builder();
    }

    protected ImmutableList.Builder<Object> list() {
        return ImmutableList.builder();
    }
}
