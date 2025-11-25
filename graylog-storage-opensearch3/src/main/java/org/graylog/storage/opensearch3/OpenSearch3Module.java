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

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.shaded.opensearch2.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.storage.opensearch3.client.IndexerHostsAdapterOS2;
import org.graylog.storage.opensearch3.client.OSCredentialsProvider;
import org.graylog.storage.opensearch3.client.OpensearchCredentialsProvider;
import org.graylog.storage.opensearch3.fieldtypes.streams.StreamsForFieldRetrieverOS;
import org.graylog.storage.opensearch3.indextemplates.ComposableIndexTemplateAdapter;
import org.graylog.storage.opensearch3.indextemplates.LegacyIndexTemplateAdapter;
import org.graylog.storage.opensearch3.migrations.V20170607164210_MigrateReopenedIndicesToAliasesClusterStateOS2;
import org.graylog.storage.opensearch3.sniffer.SnifferBuilder;
import org.graylog.storage.opensearch3.sniffer.SnifferFilter;
import org.graylog.storage.opensearch3.sniffer.impl.DatanodesSniffer;
import org.graylog.storage.opensearch3.sniffer.impl.NodeAttributesFilter;
import org.graylog.storage.opensearch3.sniffer.impl.NodeLoggingFilter;
import org.graylog.storage.opensearch3.sniffer.impl.OpensearchClusterSniffer;
import org.graylog.storage.opensearch3.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEventsOS2;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.client.IndexerHostsAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.security.SecurityAdapter;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.graylog2.plugin.VersionAwareModule;
import org.graylog2.storage.SearchVersion;

public class OpenSearch3Module extends VersionAwareModule {
    private final SearchVersion supportedVersion;
    private final boolean useComposableIndexTemplates;

    public OpenSearch3Module(final SearchVersion supportedVersion, boolean useComposableIndexTemplates) {
        this.supportedVersion = supportedVersion;
        this.useComposableIndexTemplates = useComposableIndexTemplates;
    }

    @Override
    protected void configure() {
        bindForSupportedVersion(StreamsForFieldRetriever.class).to(StreamsForFieldRetrieverOS.class);
        bindForSupportedVersion(CountsAdapter.class).to(CountsAdapterOS.class);
        bindForSupportedVersion(ClusterAdapter.class).to(ClusterAdapterOS.class);
        bindForSupportedVersion(IndicesAdapter.class).to(IndicesAdapterOS.class);
        bindForSupportedVersion(DataStreamAdapter.class).to(DataStreamAdapterOS.class);
        bindForSupportedVersion(SecurityAdapter.class).to(SecurityAdapterOS.class);
        if (useComposableIndexTemplates) {
            bindForSupportedVersion(IndexTemplateAdapter.class).to(ComposableIndexTemplateAdapter.class);
        } else {
            bindForSupportedVersion(IndexTemplateAdapter.class).to(LegacyIndexTemplateAdapter.class);
        }
        bindForSupportedVersion(IndexFieldTypePollerAdapter.class).to(IndexFieldTypePollerAdapterOS.class);
        bindForSupportedVersion(IndexToolsAdapter.class).to(IndexToolsAdapterOS2.class);
        bindForSupportedVersion(MessagesAdapter.class).to(MessagesAdapterOS2.class);
        bindForSupportedVersion(MultiChunkResultRetriever.class).to(PaginationOS2.class);
        bindForSupportedVersion(MoreSearchAdapter.class).to(MoreSearchAdapterOS2.class);
        bindForSupportedVersion(NodeAdapter.class).to(NodeAdapterOS.class);
        bindForSupportedVersion(SearchesAdapter.class).to(SearchesAdapterOS.class);
        bindForSupportedVersion(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class)
                .to(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateOS2.class);
        bindForSupportedVersion(V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter.class)
                .to(V20200730000000_AddGl2MessageIdFieldAliasForEventsOS2.class);

        bindForSupportedVersion(QuerySuggestionsService.class).to(QuerySuggestionsOS2.class);

        bindForSupportedVersion(ProxyRequestAdapter.class).to(ProxyRequestAdapterOS2.class);
        bindForSupportedVersion(RemoteReindexingMigrationAdapter.class).to(UnsupportedRemoteReindexMigrationAdapterOS.class);

        install(new FactoryModuleBuilder().build(ScrollResultOS2.Factory.class));

        bind(RestHighLevelClient.class).toProvider(RestClientProvider.class);
        bind(OfficialOpensearchClient.class).toProvider(OfficialOpensearchClientProvider.class).asEagerSingleton();
        bind(CredentialsProvider.class).toProvider(OSCredentialsProvider.class);
        bind(org.apache.hc.client5.http.auth.CredentialsProvider.class).toProvider(OpensearchCredentialsProvider.class);
        bindForSupportedVersion(DatanodeUpgradeServiceAdapter.class).to(DatanodeUpgradeServiceAdapterOS2.class);

        Multibinder<SnifferBuilder> snifferBuilders = Multibinder.newSetBinder(binder(), SnifferBuilder.class);
        snifferBuilders.addBinding().to(OpensearchClusterSniffer.class);
        snifferBuilders.addBinding().to(DatanodesSniffer.class);

        Multibinder<SnifferFilter> snifferFilters = Multibinder.newSetBinder(binder(), SnifferFilter.class);
        snifferFilters.addBinding().to(NodeAttributesFilter.class);
        snifferFilters.addBinding().to(NodeLoggingFilter.class);

        bindForSupportedVersion(IndexerHostsAdapter.class).to(IndexerHostsAdapterOS2.class);
    }

    private <T> LinkedBindingBuilder<T> bindForSupportedVersion(Class<T> interfaceClass) {
        return bindForVersion(supportedVersion, interfaceClass);
    }
}
