package org.graylog.storage.elasticsearch6;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
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
import org.graylog2.plugin.Version;

public class Elasticsearch6Module extends PluginModule {
    private static final Version SUPPORTED_VERSION = Version.from(6, 0, 0);
    @Override
    protected void configure() {
        bindForVersion(CountsAdapter.class, CountsAdapterES6.class);
        bindForVersion(IndicesAdapter.class, IndicesAdapterES6.class);
        bindForVersion(SearchesAdapter.class, SearchesAdapterES6.class);
        bindForVersion(MoreSearchAdapter.class, MoreSearchAdapterES6.class);
        bindForVersion(MessagesAdapter.class, MessagesAdapterES6.class);
        bindForVersion(ClusterAdapter.class, ClusterAdapterES6.class);
        bindForVersion(NodeAdapter.class, NodeAdapterES6.class);
        bindForVersion(EventIndexerAdapter.class, EventIndexerAdapterES6.class);
        bindForVersion(IndexFieldTypePollerAdapter.class, IndexFieldTypePollerAdapterES6.class);
        bindForVersion(IndexToolsAdapter.class, IndexToolsAdapterES6.class);
        bindForVersion(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class, V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6.class);

        install(new FactoryModuleBuilder().build(ScrollResultES6.Factory.class));

        bind(JestClient.class).toProvider(JestClientProvider.class).asEagerSingleton();
    }

    private <T> void bindForVersion(Class<T> interfaceClass, Class<? extends T> implementationClass) {
        mapBinder(interfaceClass).addBinding(SUPPORTED_VERSION).to(implementationClass);
    }

    private <T> MapBinder<Version, T> mapBinder(Class<T> interfaceClass) {
        return MapBinder.newMapBinder(binder(), Version.class, interfaceClass);
    }
}
