package org.graylog.storage.elasticsearch6;

import com.google.inject.assistedinject.FactoryModuleBuilder;
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
import org.graylog2.plugin.PluginModule;

public class Elasticsearch6Module extends PluginModule {
    @Override
    protected void configure() {
        bind(CountsAdapter.class).to(CountsAdapterES6.class);
        bind(IndicesAdapter.class).to(IndicesAdapterES6.class);
        bind(SearchesAdapter.class).to(SearchesAdapterES6.class);
        bind(MoreSearchAdapter.class).to(MoreSearchAdapterES6.class);
        bind(MessagesAdapter.class).to(MessagesAdapterES6.class);
        bind(ClusterAdapter.class).to(ClusterAdapterES6.class);
        bind(NodeAdapter.class).to(NodeAdapterES6.class);
        bind(EventIndexerAdapter.class).to(EventIndexerAdapterES6.class);
        bind(IndexFieldTypePollerAdapter.class).to(IndexFieldTypePollerAdapterES6.class);
        bind(IndexToolsAdapter.class).to(IndexToolsAdapterES6.class);
        bind(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class).to(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6.class);

        install(new FactoryModuleBuilder().build(ScrollResultES6.Factory.class));

        bind(JestClient.class).toProvider(JestClientProvider.class).asEagerSingleton();
    }
}
