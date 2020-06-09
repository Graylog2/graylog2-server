package org.graylog.storage.elasticsearch6;

import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.plugin.PluginModule;

public class Elasticsearch6Module extends PluginModule {
    @Override
    protected void configure() {
        bind(SearchesAdapter.class).to(SearchesAdapterES6.class);
        bind(MoreSearchAdapter.class).to(MoreSearchAdapterES6.class);
        bind(IndicesAdapter.class).to(IndicesAdapterES6.class);
        bind(ClusterAdapter.class).to(ClusterAdapterES6.class);
        bind(MessagesAdapter.class).to(MessagesAdapterES6.class);
    }
}
