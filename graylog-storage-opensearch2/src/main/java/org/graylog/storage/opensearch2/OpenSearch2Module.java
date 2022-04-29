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
package org.graylog.storage.opensearch2;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.shaded.opensearch2.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.storage.opensearch2.client.OSCredentialsProvider;
import org.graylog.storage.opensearch2.fieldtypes.streams.StreamsWithFieldUsageRetrieverOS2;
import org.graylog.storage.opensearch2.migrations.V20170607164210_MigrateReopenedIndicesToAliasesClusterStateOS2;
import org.graylog.storage.opensearch2.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEventsOS2;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsWithFieldUsageRetriever;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.graylog2.plugin.VersionAwareModule;
import org.graylog2.storage.SearchVersion;

public class OpenSearch2Module extends VersionAwareModule {

    private final SearchVersion supportedVersion;

    public OpenSearch2Module(final SearchVersion supportedVersion) {
        this.supportedVersion = supportedVersion;
    }

    @Override
    protected void configure() {
        bindForSupportedVersion(StreamsWithFieldUsageRetriever.class).to(StreamsWithFieldUsageRetrieverOS2.class);
        bindForSupportedVersion(CountsAdapter.class).to(CountsAdapterOS2.class);
        bindForSupportedVersion(ClusterAdapter.class).to(ClusterAdapterOS2.class);
        bindForSupportedVersion(IndicesAdapter.class).to(IndicesAdapterOS2.class);
        bindForSupportedVersion(IndexFieldTypePollerAdapter.class).to(IndexFieldTypePollerAdapterOS2.class);
        bindForSupportedVersion(IndexToolsAdapter.class).to(IndexToolsAdapterOS2.class);
        bindForSupportedVersion(MessagesAdapter.class).to(MessagesAdapterOS2.class);
        bindForSupportedVersion(MoreSearchAdapter.class).to(MoreSearchAdapterOS2.class);
        bindForSupportedVersion(NodeAdapter.class).to(NodeAdapterOS2.class);
        bindForSupportedVersion(SearchesAdapter.class).to(SearchesAdapterOS2.class);
        bindForSupportedVersion(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class)
                .to(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateOS2.class);
        bindForSupportedVersion(V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter.class)
                .to(V20200730000000_AddGl2MessageIdFieldAliasForEventsOS2.class);

        bindForSupportedVersion(QuerySuggestionsService.class).to(QuerySuggestionsOS2.class);

        install(new FactoryModuleBuilder().build(ScrollResultOS2.Factory.class));

        bind(RestHighLevelClient.class).toProvider(RestHighLevelClientProvider.class);
        bind(CredentialsProvider.class).toProvider(OSCredentialsProvider.class);
    }

    private <T> LinkedBindingBuilder<T> bindForSupportedVersion(Class<T> interfaceClass) {
        return bindForVersion(supportedVersion, interfaceClass);
    }
}
