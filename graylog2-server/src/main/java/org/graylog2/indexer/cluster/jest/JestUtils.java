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
package org.graylog2.indexer.cluster.jest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.http.JestHttpClient;
import org.apache.http.client.config.RequestConfig;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.FieldTypeException;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.QueryParsingException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class JestUtils {
    private JestUtils() {
    }

    public static <T extends JestResult> T execute(JestClient client, RequestConfig requestConfig,
                                                   Action<T> request, Supplier<String> errorMessage) {
        final T result;
        try {
            if (client instanceof JestHttpClient) {
                result = ((JestHttpClient) client).execute(request, requestConfig);
            } else {
                result = client.execute(request);
            }
        } catch (IOException e) {
            throw new ElasticsearchException(errorMessage.get(), e);
        }

        if (result.isSucceeded()) {
            return result;
        } else {
            throw specificException(errorMessage, result.getJsonObject());
        }
    }

    public static <T extends JestResult> T execute(JestClient client, Action<T> request, Supplier<String> errorMessage) {
        return execute(client, null, request, errorMessage);
    }

    private static ElasticsearchException specificException(Supplier<String> errorMessage, JsonNode jsonObject) {
        final List<JsonNode> rootCauses = extractRootCauses(jsonObject);
        final List<String> reasons = extractReasons(rootCauses);

        for (JsonNode rootCause : rootCauses) {
            final String type = rootCause.path("type").asText(null);
            if (type == null) {
                continue;
            }
            switch(type) {
                case "query_parsing_exception":
                    return buildQueryParsingException(errorMessage, rootCause, reasons);
                case "index_not_found_exception":
                    final String indexName = rootCause.path("resource.id").asText();
                    return buildIndexNotFoundException(errorMessage, indexName);
                case "illegal_argument_exception":
                    final String reason = rootCause.path("reason").asText();
                    if (reason.startsWith("Expected numeric type on field")) {
                        return buildFieldTypeException(errorMessage, reason);
                    }
                    break;
            }
        }

        if (reasons.isEmpty()) {
            return new ElasticsearchException(errorMessage.get(), Collections.singletonList(jsonObject.toString()));
        }

        return new ElasticsearchException(errorMessage.get(), reasons);
    }

    private static FieldTypeException buildFieldTypeException(Supplier<String> errorMessage, String reason) {
        return new FieldTypeException(errorMessage.get(), reason);
    }

    private static List<String> extractReasons(List<JsonNode> rootCauses) {
        return rootCauses.stream()
                .map(rootCause -> rootCause.path("reason").asText(null))
                .collect(Collectors.toList());
    }

    private static List<JsonNode> extractRootCauses(JsonNode jsonObject) {
        return ImmutableList.copyOf(jsonObject.path("error").path("root_cause").iterator());
    }

    private static QueryParsingException buildQueryParsingException(Supplier<String> errorMessage,
                                                                    JsonNode rootCause,
                                                                    List<String> reasons) {
        final JsonNode lineJson = rootCause.path("line");
        final Integer line = lineJson.isInt() ? lineJson.asInt() : null;
        final JsonNode columnJson = rootCause.path("col");
        final Integer column = columnJson.isInt() ? columnJson.asInt() : null;
        final String index = rootCause.path("index").asText(null);

        return new QueryParsingException(errorMessage.get(), line, column, index, reasons);
    }

    private static IndexNotFoundException buildIndexNotFoundException(Supplier<String> errorMessage, String index) {
        return new IndexNotFoundException(errorMessage.get(), Collections.singletonList("Index not found for query: " + index + ". Try recalculating your index ranges."));
    }
}
