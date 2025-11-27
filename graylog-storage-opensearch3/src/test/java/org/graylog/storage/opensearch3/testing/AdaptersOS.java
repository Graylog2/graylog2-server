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
package org.graylog.storage.opensearch3.testing;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.storage.opensearch3.CountsAdapterOS;
import org.graylog.storage.opensearch3.IndexFieldTypePollerAdapterOS;
import org.graylog.storage.opensearch3.IndexToolsAdapterOS2;
import org.graylog.storage.opensearch3.IndicesAdapterOS;
import org.graylog.storage.opensearch3.MessagesAdapterOS2;
import org.graylog.storage.opensearch3.NodeAdapterOS;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.OpenSearchClient;
import org.graylog.storage.opensearch3.PlainJsonApi;
import org.graylog.storage.opensearch3.Scroll;
import org.graylog.storage.opensearch3.ScrollResultOS2;
import org.graylog.storage.opensearch3.SearchRequestFactory;
import org.graylog.storage.opensearch3.SearchRequestFactoryOS;
import org.graylog.storage.opensearch3.SearchesAdapterOS;
import org.graylog.storage.opensearch3.fieldtypes.streams.StreamsForFieldRetrieverOS;
import org.graylog.storage.opensearch3.indextemplates.ComposableIndexTemplateAdapter;
import org.graylog.storage.opensearch3.indextemplates.LegacyIndexTemplateAdapter;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.mapping.FieldMappingApi;
import org.graylog.storage.opensearch3.stats.IndexStatisticsBuilder;
import org.graylog.testing.elasticsearch.Adapters;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.indexer.results.TestResultMessageFactory;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.util.List;

import static org.graylog2.indexer.Constants.COMPOSABLE_INDEX_TEMPLATES_FEATURE;

public class AdaptersOS implements Adapters {

    @Deprecated
    private final OpenSearchClient client;
    private final OfficialOpensearchClient officialOpensearchClient;
    private final List<String> featureFlags;
    private final ObjectMapper objectMapper;
    private final ResultMessageFactory resultMessageFactory = new TestResultMessageFactory();
    private final OSSerializationUtils osSerializationUtils;
    private final SearchRequestFactory searchRequestFactory;

    public AdaptersOS(@Deprecated OpenSearchClient client, OfficialOpensearchClient officialOpensearchClient, List<String> featureFlags) {
        this.client = client;
        this.officialOpensearchClient = officialOpensearchClient;
        this.featureFlags = featureFlags;
        this.objectMapper = new ObjectMapperProvider().get();
        osSerializationUtils = new OSSerializationUtils();
        this.searchRequestFactory = new SearchRequestFactory(true, true, new IgnoreSearchFilters());
    }


    @Override
    public CountsAdapter countsAdapter() {
        return new CountsAdapterOS(officialOpensearchClient, new SearchRequestFactoryOS(true));
    }

    @Override
    public IndicesAdapter indicesAdapter() {
        return new IndicesAdapterOS(officialOpensearchClient,
                new org.graylog.storage.opensearch3.stats.StatsApi(officialOpensearchClient),
                new org.graylog.storage.opensearch3.stats.ClusterStatsApi(officialOpensearchClient),
                new org.graylog.storage.opensearch3.cluster.ClusterStateApi(officialOpensearchClient),
                indexTemplateAdapter(),
                new IndexStatisticsBuilder(),
                objectMapper,
                new PlainJsonApi(objectMapper, client, officialOpensearchClient),
                osSerializationUtils
        );
    }

    @Override
    public NodeAdapter nodeAdapter() {
        return new NodeAdapterOS(officialOpensearchClient);
    }

    @Override
    public IndexToolsAdapter indexToolsAdapter() {
        return new IndexToolsAdapterOS2(client);
    }

    @Override
    public SearchesAdapter searchesAdapter() {
        final ScrollResultOS2.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultOS2(
                resultMessageFactory, client, initialResult, query, scroll, fields, limit
        );
        return new SearchesAdapterOS(client,
                new Scroll(client,
                        scrollResultFactory,
                        searchRequestFactory),
                searchRequestFactory, resultMessageFactory);
    }

    @Override
    public MessagesAdapter messagesAdapter() {
        return new MessagesAdapterOS2(resultMessageFactory, client, new MetricRegistry(), new ChunkedBulkIndexer(), objectMapper);
    }

    @Override
    public IndexFieldTypePollerAdapter indexFieldTypePollerAdapter() {
        return indexFieldTypePollerAdapter(new Configuration());
    }

    @Override
    public IndexFieldTypePollerAdapter indexFieldTypePollerAdapter(final Configuration configuration) {
        return new IndexFieldTypePollerAdapterOS(new FieldMappingApi(officialOpensearchClient), configuration, new StreamsForFieldRetrieverOS(officialOpensearchClient));
    }

    @Override
    public IndexTemplateAdapter indexTemplateAdapter() {
        if(featureFlags.contains(COMPOSABLE_INDEX_TEMPLATES_FEATURE)) {
            return new ComposableIndexTemplateAdapter(officialOpensearchClient, osSerializationUtils);
        } else {
            return new LegacyIndexTemplateAdapter(officialOpensearchClient, osSerializationUtils);
        }
    }
}
