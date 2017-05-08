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

import com.google.gson.JsonObject;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.http.JestHttpClient;
import org.apache.http.client.config.RequestConfig;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.QueryParsingException;
import org.graylog2.indexer.gson.GsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.graylog2.indexer.gson.GsonUtils.asInteger;
import static org.graylog2.indexer.gson.GsonUtils.asJsonArray;
import static org.graylog2.indexer.gson.GsonUtils.asJsonObject;
import static org.graylog2.indexer.gson.GsonUtils.asString;

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

    private static ElasticsearchException specificException(Supplier<String> errorMessage, JsonObject jsonObject) {
        final List<JsonObject> rootCauses = extractRootCauses(jsonObject);
        final List<String> reasons = extractReasons(rootCauses);

        for (JsonObject rootCause : rootCauses) {
            final String type = asString(rootCause.get("type"));
            switch(type) {
                case "query_parsing_exception":
                    return buildQueryParsingException(errorMessage, rootCause, reasons);
                case "index_not_found_exception":
                    final String indexName = asString(rootCause.get("resource.id"));
                    return new ElasticsearchException("Index not found for query: " + indexName + ". Try recalculating your index ranges.");
            }
        }

        if (reasons.isEmpty()) {
            return new ElasticsearchException(errorMessage.get(), Collections.singletonList(jsonObject.toString()));
        }

        return new ElasticsearchException(errorMessage.get(), reasons);
    }

    private static List<String> extractReasons(List<JsonObject> rootCauses) {
        return rootCauses.stream()
                .map(rootCause -> asString(rootCause.get("reason")))
                .collect(Collectors.toList());
    }

    private static List<JsonObject> extractRootCauses(JsonObject jsonObject) {
        return Optional.of(jsonObject)
                .map(json -> asJsonObject(json.get("error")))
                .map(error -> asJsonArray(error.get("root_cause")))
                .map(Iterable::spliterator)
                .map(x -> StreamSupport.stream(x, false))
                .orElse(Stream.empty())
                .map(GsonUtils::asJsonObject)
                .collect(Collectors.toList());
    }

    private static QueryParsingException buildQueryParsingException(Supplier<String> errorMessage,
                                                                    JsonObject rootCause,
                                                                    List<String> reasons) {
        final Integer line = asInteger(rootCause.get("line"));
        final Integer column = asInteger(rootCause.get("col"));
        final String index = asString(rootCause.get("index"));

        return new QueryParsingException(errorMessage.get(), line, column, index, reasons);
    }
}
