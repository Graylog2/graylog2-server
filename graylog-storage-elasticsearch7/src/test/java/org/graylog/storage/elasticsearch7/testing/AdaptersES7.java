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
package org.graylog.storage.elasticsearch7.testing;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.elasticsearch7.CountsAdapterES7;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.IndexFieldTypePollerAdapterES7;
import org.graylog.storage.elasticsearch7.IndexToolsAdapterES7;
import org.graylog.storage.elasticsearch7.IndicesAdapterES7;
import org.graylog.storage.elasticsearch7.MessagesAdapterES7;
import org.graylog.storage.elasticsearch7.NodeAdapterES7;
import org.graylog.storage.elasticsearch7.Scroll;
import org.graylog.storage.elasticsearch7.ScrollResultES7;
import org.graylog.storage.elasticsearch7.SearchRequestFactory;
import org.graylog.storage.elasticsearch7.SearchesAdapterES7;
import org.graylog.storage.elasticsearch7.SortOrderMapper;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cluster.ClusterStateApi;
import org.graylog.storage.elasticsearch7.fieldtypes.streams.StreamsForFieldRetrieverES7;
import org.graylog.storage.elasticsearch7.mapping.FieldMappingApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
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

public class AdaptersES7 implements Adapters {

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;

    public AdaptersES7(ElasticsearchClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapperProvider().get();
    }

    @Override
    public CountsAdapter countsAdapter() {
        return new CountsAdapterES7(client);
    }

    @Override
    public IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES7(
                client,
                new StatsApi(objectMapper, client),
                new CatApi(objectMapper, client),
                new ClusterStateApi(objectMapper, client)
        );
    }

    @Override
    public NodeAdapter nodeAdapter() {
        return new NodeAdapterES7(client, objectMapper);
    }

    @Override
    public IndexToolsAdapter indexToolsAdapter() {
        return new IndexToolsAdapterES7(client);
    }

    @Override
    public SearchesAdapter searchesAdapter() {
        final ScrollResultES7.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultES7(
                client, initialResult, query, scroll, fields, limit
        );
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final boolean allowHighlighting = true;
        final boolean allowLeadingWildcardSearches = true;

        final SearchRequestFactory searchRequestFactory = new SearchRequestFactory(sortOrderMapper, allowHighlighting, allowLeadingWildcardSearches);
        final Scroll scroll = new Scroll(client, scrollResultFactory, searchRequestFactory);
        return new SearchesAdapterES7(client, scroll, searchRequestFactory);
    }

    @Override
    public MessagesAdapter messagesAdapter() {
        return new MessagesAdapterES7(client, new MetricRegistry(), new ChunkedBulkIndexer(), objectMapper);
    }

    @Override
    public IndexFieldTypePollerAdapter indexFieldTypePollerAdapter() {
        return indexFieldTypePollerAdapter(new Configuration());
    }

    @Override
    public IndexFieldTypePollerAdapter indexFieldTypePollerAdapter(final Configuration configuration) {
        return new IndexFieldTypePollerAdapterES7(new FieldMappingApi(objectMapper, client), configuration, new StreamsForFieldRetrieverES7(client));
    }
}
