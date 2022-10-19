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
package org.graylog.storage.opensearch2.testing;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch2.CountsAdapterOS2;
import org.graylog.storage.opensearch2.IndexFieldTypePollerAdapterOS2;
import org.graylog.storage.opensearch2.IndexToolsAdapterOS2;
import org.graylog.storage.opensearch2.IndicesAdapterOS2;
import org.graylog.storage.opensearch2.MessagesAdapterOS2;
import org.graylog.storage.opensearch2.NodeAdapterOS2;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.Scroll;
import org.graylog.storage.opensearch2.ScrollResultOS2;
import org.graylog.storage.opensearch2.SearchRequestFactory;
import org.graylog.storage.opensearch2.SearchesAdapterOS2;
import org.graylog.storage.opensearch2.SortOrderMapper;
import org.graylog.storage.opensearch2.fieldtypes.streams.StreamsForFieldRetrieverOS2;
import org.graylog.storage.opensearch2.mapping.FieldMappingApi;
import org.graylog.testing.elasticsearch.Adapters;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

public class AdaptersOS2 implements Adapters {

    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;

    public AdaptersOS2(OpenSearchClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapperProvider().get();
    }

    @Override
    public CountsAdapter countsAdapter() {
        return new CountsAdapterOS2(client);
    }

    @Override
    public IndicesAdapter indicesAdapter() {
        return new IndicesAdapterOS2(client,
                new org.graylog.storage.opensearch2.stats.StatsApi(objectMapper, client),
                new org.graylog.storage.opensearch2.cat.CatApi(objectMapper, client),
                new org.graylog.storage.opensearch2.cluster.ClusterStateApi(objectMapper, client)
        );
    }

    @Override
    public NodeAdapter nodeAdapter() {
        return new NodeAdapterOS2(client, objectMapper);
    }

    @Override
    public IndexToolsAdapter indexToolsAdapter() {
        return new IndexToolsAdapterOS2(client);
    }

    @Override
    public SearchesAdapter searchesAdapter() {
        final ScrollResultOS2.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultOS2(
                client, initialResult, query, scroll, fields, limit
        );
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final boolean allowHighlighting = true;
        final boolean allowLeadingWildcardSearches = true;

        final SearchRequestFactory searchRequestFactory = new SearchRequestFactory(sortOrderMapper, allowHighlighting, allowLeadingWildcardSearches);
        return new SearchesAdapterOS2(client,
                new Scroll(client,
                        scrollResultFactory,
                        searchRequestFactory),
                searchRequestFactory);
    }

    @Override
    public MessagesAdapter messagesAdapter() {
        return new MessagesAdapterOS2(client, new MetricRegistry(), new ChunkedBulkIndexer(), objectMapper);
    }

    @Override
    public IndexFieldTypePollerAdapter indexFieldTypePollerAdapter() {
        return indexFieldTypePollerAdapter(new Configuration());
    }

    @Override
    public IndexFieldTypePollerAdapter indexFieldTypePollerAdapter(final Configuration configuration) {
        return new IndexFieldTypePollerAdapterOS2(new FieldMappingApi(objectMapper, client), configuration, new StreamsForFieldRetrieverOS2(client));
    }

}
