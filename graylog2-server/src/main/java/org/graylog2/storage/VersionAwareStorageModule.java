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
package org.graylog2.storage;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog.plugins.views.search.engine.monitoring.collection.InMemoryCappedStatsCollector;
import org.graylog.plugins.views.search.engine.monitoring.collection.NoOpStatsCollector;
import org.graylog.plugins.views.search.engine.monitoring.collection.StatsCollector;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.graylog2.storage.providers.ClusterAdapterProvider;
import org.graylog2.storage.providers.CountsAdapterProvider;
import org.graylog2.storage.providers.DataStreamAdapterProvider;
import org.graylog2.storage.providers.ElasticsearchBackendProvider;
import org.graylog2.storage.providers.IndexFieldTypePollerAdapterProvider;
import org.graylog2.storage.providers.IndexToolsAdapterProvider;
import org.graylog2.storage.providers.IndicesAdapterProvider;
import org.graylog2.storage.providers.MessagesAdapterProvider;
import org.graylog2.storage.providers.MoreSearchAdapterProvider;
import org.graylog2.storage.providers.MultiChunkResultRetrieverProvider;
import org.graylog2.storage.providers.NodeAdapterProvider;
import org.graylog2.storage.providers.ProxyRequestAdapterProvider;
import org.graylog2.storage.providers.RemoteReindexingMigrationAdapterProvider;
import org.graylog2.storage.providers.SearchesAdapterProvider;
import org.graylog2.storage.providers.StreamsForFieldRetrieverProvider;
import org.graylog2.storage.providers.V20170607164210_MigrateReopenedIndicesToAliasesClusterStateAdapterProvider;
import org.graylog2.storage.providers.V20200730000000_AddGl2MessageIdFieldAliasForEventsElasticsearchAdapterProvider;


public class VersionAwareStorageModule extends AbstractModule {

    private final Configuration configuration;

    public VersionAwareStorageModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bind(StreamsForFieldRetriever.class).toProvider(StreamsForFieldRetrieverProvider.class);
        bind(CountsAdapter.class).toProvider(CountsAdapterProvider.class);
        bind(IndicesAdapter.class).toProvider(IndicesAdapterProvider.class);
        bind(DataStreamAdapter.class).toProvider(DataStreamAdapterProvider.class);
        bind(SearchesAdapter.class).toProvider(SearchesAdapterProvider.class);
        bind(MultiChunkResultRetriever.class).toProvider(MultiChunkResultRetrieverProvider.class);
        bind(MoreSearchAdapter.class).toProvider(MoreSearchAdapterProvider.class);
        bind(MessagesAdapter.class).toProvider(MessagesAdapterProvider.class);
        bind(ClusterAdapter.class).toProvider(ClusterAdapterProvider.class);
        bind(NodeAdapter.class).toProvider(NodeAdapterProvider.class);
        bind(IndexFieldTypePollerAdapter.class).toProvider(IndexFieldTypePollerAdapterProvider.class);
        bind(IndexToolsAdapter.class).toProvider(IndexToolsAdapterProvider.class);
        bind(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class)
                .toProvider(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateAdapterProvider.class);
        bind(V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter.class)
                .toProvider(V20200730000000_AddGl2MessageIdFieldAliasForEventsElasticsearchAdapterProvider.class);
        bind(ProxyRequestAdapter.class).toProvider(ProxyRequestAdapterProvider.class);
        bind(RemoteReindexingMigrationAdapter.class).toProvider(RemoteReindexingMigrationAdapterProvider.class);

        bindQueryBackend();
    }

    private void bindQueryBackend() {
        if (configuration.isQueryLatencyMonitoringEnabled() && configuration.getQueryLatencyMonitoringWindowSize() > 0) {
            bind(new TypeLiteral<StatsCollector<QueryExecutionStats>>() {})
                    .toInstance(new InMemoryCappedStatsCollector<>(configuration.getQueryLatencyMonitoringWindowSize()));
        } else {
            bind(new TypeLiteral<StatsCollector<QueryExecutionStats>>() {})
                    .toInstance(new NoOpStatsCollector<>());
        }
        bind(new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {})
                .toProvider(ElasticsearchBackendProvider.class);
    }
}
