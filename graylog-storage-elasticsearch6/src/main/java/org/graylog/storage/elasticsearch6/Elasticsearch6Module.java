package org.graylog.storage.elasticsearch6;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import io.searchbox.client.JestClient;
import org.graylog.events.indices.EventIndexerAdapter;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.storage.elasticsearch6.migrations.V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6;
import org.graylog.storage.elasticsearch6.jest.JestClientProvider;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.graylog2.plugin.VersionAwareModule;

import static org.graylog.storage.elasticsearch6.Elasticsearch6Plugin.SUPPORTED_ES_VERSION;

public class Elasticsearch6Module extends VersionAwareModule {
    @Override
    protected void configure() {
        bindForSupportedVersion(CountsAdapter.class).to(CountsAdapterES6.class);
        bindForSupportedVersion(IndicesAdapter.class).to(IndicesAdapterES6.class);
        bindForSupportedVersion(SearchesAdapter.class).to(SearchesAdapterES6.class);
        bindForSupportedVersion(MoreSearchAdapter.class).to(MoreSearchAdapterES6.class);
        bindForSupportedVersion(MessagesAdapter.class).to(MessagesAdapterES6.class);
        bindForSupportedVersion(ClusterAdapter.class).to(ClusterAdapterES6.class);
        bindForSupportedVersion(NodeAdapter.class).to(NodeAdapterES6.class);
        bindForSupportedVersion(EventIndexerAdapter.class).to(EventIndexerAdapterES6.class);
        bindForSupportedVersion(IndexFieldTypePollerAdapter.class).to(IndexFieldTypePollerAdapterES6.class);
        bindForSupportedVersion(IndexToolsAdapter.class).to(IndexToolsAdapterES6.class);
        bindForSupportedVersion(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class).to(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6.class);

        install(new FactoryModuleBuilder().build(ScrollResultES6.Factory.class));

        bind(JestClient.class).toProvider(JestClientProvider.class).asEagerSingleton();
    }

    private <T> LinkedBindingBuilder<T> bindForSupportedVersion(Class<T> interfaceClass) {
        return bindForVersion(SUPPORTED_ES_VERSION, interfaceClass);
    }
}
