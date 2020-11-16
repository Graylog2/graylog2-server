/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch6.jest;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


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
            throw specificException(errorMessage, result.getJsonObject().path("error"));
        }
    }

    public static <T extends JestResult> T execute(JestClient client, Action<T> request, Supplier<String> errorMessage) {
        return execute(client, null, request, errorMessage);
    }

    public static <T extends JestResult> T execute(JestClient client, RequestConfig requestConfig,
                                                   Action<T> request) throws IOException {
        if (client instanceof JestHttpClient) {
            return ((JestHttpClient) client).execute(request, requestConfig);
        } else {
            return client.execute(request);
        }
    }

    public static ElasticsearchException specificException(Supplier<String> errorMessage, JsonNode errorNode) {
        final JsonNode rootCauses = errorNode.path("root_cause");
        final List<String> reasons = new ArrayList<>(rootCauses.size());

        for (JsonNode rootCause : rootCauses) {
            final JsonNode reason = rootCause.path("reason");
            if (reason.isTextual()) {
                reasons.add(reason.asText());
            }

            final JsonNode type = rootCause.path("type");
            if (!type.isTextual()) {
                continue;
            }
            switch(type.asText()) {
                case "query_parsing_exception":
                    return buildQueryParsingException(errorMessage, rootCause, reasons);
                case "index_not_found_exception":
                    final String indexName = rootCause.path("resource.id").asText();
                    return IndexNotFoundException.create(errorMessage.get(), indexName);
                case "illegal_argument_exception":
                    final String reasonText = reason.asText();
                    if (reasonText.startsWith("Expected numeric type on field")) {
                        return buildFieldTypeException(errorMessage, reasonText);
                    }
                    break;
            }
        }

        if (reasons.isEmpty()) {
            return new ElasticsearchException(errorMessage.get(), Collections.singletonList(errorNode.toString()));
        }

        return new ElasticsearchException(errorMessage.get(), deduplicateErrors(reasons));
    }

    public static List<String> deduplicateErrors(List<String> errors) {
        return errors.stream().distinct().collect(Collectors.toList());
    }

    private static FieldTypeException buildFieldTypeException(Supplier<String> errorMessage, String reason) {
        return new FieldTypeException(errorMessage.get(), reason);
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

    public static Optional<ElasticsearchException> checkForFailedShards(JestResult result) {
        // unwrap shard failure due to non-numeric mapping. this happens when searching across index sets
        // if at least one of the index sets comes back with a result, the overall result will have the aggregation
        // but not considered failed entirely. however, if one shard has the error, we will refuse to respond
        // otherwise we would be showing empty graphs for non-numeric fields.
        final JsonNode shards = result.getJsonObject().path("_shards");
        final double failedShards = shards.path("failed").asDouble();

        if (failedShards > 0) {
            final List<String> errors = StreamSupport.stream(shards.path("failures").spliterator(), false)
                    .map(failure -> failure.path("reason").path("reason").asText())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            final List<String> nonNumericFieldErrors = errors.stream()
                    .filter(error -> error.startsWith("Expected numeric type on field"))
                    .collect(Collectors.toList());
            if (!nonNumericFieldErrors.isEmpty()) {
                return Optional.of(new FieldTypeException("Unable to perform search query: ", deduplicateErrors(nonNumericFieldErrors)));
            }

            return Optional.of(new ElasticsearchException("Unable to perform search query: ", deduplicateErrors(errors)));
        }

        return Optional.empty();
    }
}
