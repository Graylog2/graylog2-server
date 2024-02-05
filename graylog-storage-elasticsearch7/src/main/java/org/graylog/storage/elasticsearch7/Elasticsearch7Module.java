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
package org.graylog.storage.elasticsearch7;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.shaded.elasticsearch7.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.storage.elasticsearch7.client.ESCredentialsProvider;
import org.graylog.storage.elasticsearch7.fieldtypes.streams.StreamsForFieldRetrieverES7;
import org.graylog.storage.elasticsearch7.migrations.V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES7;
import org.graylog.storage.elasticsearch7.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEventsES7;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.graylog2.plugin.VersionAwareModule;
import org.graylog2.storage.SearchVersion;

public class Elasticsearch7Module extends VersionAwareModule {
    private final SearchVersion supportedVersion;
    private final boolean useComposableIndexTemplates;

    public Elasticsearch7Module(final SearchVersion supportedVersion, boolean useComposableIndexTemplates) {
        this.supportedVersion = supportedVersion;
        this.useComposableIndexTemplates = useComposableIndexTemplates;
    }

    @Override
    protected void configure() {
        bindForSupportedVersion(StreamsForFieldRetriever.class).to(StreamsForFieldRetrieverES7.class);
        bindForSupportedVersion(CountsAdapter.class).to(CountsAdapterES7.class);
        bindForSupportedVersion(ClusterAdapter.class).to(ClusterAdapterES7.class);
        bindForSupportedVersion(IndicesAdapter.class).to(IndicesAdapterES7.class);
        bindForSupportedVersion(DataStreamAdapter.class).to(DataStreamAdapterES7.class);
        if (useComposableIndexTemplates) {
            bind(IndexTemplateAdapter.class).to(ComposableIndexTemplateAdapter.class);
        } else {
            bind(IndexTemplateAdapter.class).to(LegacyIndexTemplateAdapter.class);
        }

        bindForSupportedVersion(IndexFieldTypePollerAdapter.class).to(IndexFieldTypePollerAdapterES7.class);
        bindForSupportedVersion(IndexToolsAdapter.class).to(IndexToolsAdapterES7.class);
        bindForSupportedVersion(MessagesAdapter.class).to(MessagesAdapterES7.class);
        bindForSupportedVersion(MultiChunkResultRetriever.class).to(PaginationES7.class);
        bindForSupportedVersion(MoreSearchAdapter.class).to(MoreSearchAdapterES7.class);
        bindForSupportedVersion(NodeAdapter.class).to(NodeAdapterES7.class);
        bindForSupportedVersion(SearchesAdapter.class).to(SearchesAdapterES7.class);
        bindForSupportedVersion(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class)
                .to(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES7.class);
        bindForSupportedVersion(V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter.class)
                .to(V20200730000000_AddGl2MessageIdFieldAliasForEventsES7.class);

        bindForSupportedVersion(QuerySuggestionsService.class).to(QuerySuggestionsES7.class);
        bindForSupportedVersion(ProxyRequestAdapter.class).to(ProxyRequestAdapterES7.class);

        install(new FactoryModuleBuilder().build(ScrollResultES7.Factory.class));

        bind(RestHighLevelClient.class).toProvider(RestHighLevelClientProvider.class);
        bind(CredentialsProvider.class).toProvider(ESCredentialsProvider.class);
    }

    private <T> LinkedBindingBuilder<T> bindForSupportedVersion(Class<T> interfaceClass) {
        return bindForVersion(supportedVersion, interfaceClass);
    }
}
