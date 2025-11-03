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
package org.graylog.storage.opensearch3;

import com.github.joschi.jadconfig.util.Duration;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.ContentTooLongException;
import org.graylog2.indexer.BatchSizeTooLargeException;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.InvalidWriteTargetException;
import org.graylog2.indexer.MapperParsingException;
import org.graylog2.indexer.MasterNotDiscoveredException;
import org.opensearch.client.ApiClient;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Options;
import org.opensearch.client.transport.httpclient5.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

public record OfficialOpensearchClient(OpenSearchClient sync, OpenSearchAsyncClient async) {
    private static final Logger LOG = LoggerFactory.getLogger(OfficialOpensearchClient.class);
    private static final Pattern invalidWriteTarget = Pattern.compile("no write index is defined for alias \\[(?<target>[\\w_]+)\\]");

    public <T> T execute(ThrowingSupplier<T> operation, String errorMessage) {
        try {
            return operation.get();
        } catch (Throwable t) {
            throw mapException(t, errorMessage);
        }
    }

    public <T> CompletableFuture<T> executeAsync(ThrowingSupplier<CompletableFuture<T>> operation, String errorMessage) {
        try {
            return operation.get().exceptionally(ex -> {
                throw mapException(ex, errorMessage);
            });
        } catch (Throwable t) {
            throw mapException(t, errorMessage);
        }
    }

    public void close() {
        try {
            sync()._transport().close();
        } catch (IOException e) {
            LOG.error("Error closing OpenSearch client", e);
        }
        try {
            async()._transport().close();
        } catch (IOException e) {
            LOG.error("Error closing async OpenSearch client", e);
        }
    }

    public static <T extends ApiClient<?, ?>> T withTimeout(T apiClient, Duration timeout) {
        ApacheHttpClient5Options options = (ApacheHttpClient5Options) getIfNull(apiClient._transportOptions(), ApacheHttpClient5Options.DEFAULT);
        RequestConfig requestConfig = getIfNull(options.getRequestConfig(), RequestConfig.DEFAULT);
        ApacheHttpClient5Options optionsWithTimeout = options.toBuilder().setRequestConfig(
                RequestConfig
                        .copy(requestConfig)
                        .setResponseTimeout(timeout.toMilliseconds(), TimeUnit.MILLISECONDS)
                        .build()
        ).build();
        return (T) apiClient.withTransportOptions(optionsWithTimeout);
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static RuntimeException mapException(Throwable t, String message) {
        if (t instanceof OpenSearchException openSearchException) {
            if (isIndexNotFoundException(openSearchException)) {
                return new IndexNotFoundException(message, List.of(t.getMessage(), "Try recalculating your index ranges"));
            }
            if (isMasterNotDiscoveredException(openSearchException)) {
                return new MasterNotDiscoveredException();
            }
            if (isInvalidWriteTargetException(openSearchException)) {
                final Matcher matcher = invalidWriteTarget.matcher(openSearchException.getMessage());
                if (matcher.find()) {
                    final String target = matcher.group("target");
                    return InvalidWriteTargetException.create(target);
                }
            }
            if (isBatchSizeTooLargeException(openSearchException)) {
                return new BatchSizeTooLargeException(openSearchException.getMessage());
            }
            if (isMapperParsingExceptionException(openSearchException)) {
                return new MapperParsingException(openSearchException.getMessage());
            }
        } else if (t instanceof ResponseException responseException) {
            if (responseException.status() == 429) {
                return new BatchSizeTooLargeException(t.getMessage());
            }
        } else if (t instanceof IOException && t.getCause() instanceof ContentTooLongException) {
            return new BatchSizeTooLargeException(t.getMessage());
        }
        return new RuntimeException(message, t);
    }

    private static boolean isInvalidWriteTargetException(OpenSearchException openSearchException) {
        try {
            final ParsedOpenSearchException parsedException = ParsedOpenSearchException.from(openSearchException.getMessage());
            return parsedException.reason().startsWith("no write index is defined for alias");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isMasterNotDiscoveredException(OpenSearchException openSearchException) {
        try {
            final ParsedOpenSearchException parsedException = ParsedOpenSearchException.from(openSearchException.getMessage());
            return parsedException.type().equals("master_not_discovered_exception")
                    || (parsedException.type().equals("cluster_block_exception") && parsedException.reason().contains("no master"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isIndexNotFoundException(OpenSearchException openSearchException) {
        return openSearchException.getMessage().contains("index_not_found_exception");
    }

    private static boolean isMapperParsingExceptionException(OpenSearchException openSearchException) {
        return openSearchException.getMessage().contains("mapper_parsing_exception");
    }

    private static boolean isBatchSizeTooLargeException(OpenSearchException openSearchException) {
        try {
            final ParsedOpenSearchException parsedException = ParsedOpenSearchException.from(openSearchException.getMessage());
            if (parsedException.type().equals("search_phase_execution_exception")) {
                ParsedOpenSearchException parsedCause = ParsedOpenSearchException.from(openSearchException.getCause().getMessage());
                return parsedCause.reason().contains("Batch size is too large");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
