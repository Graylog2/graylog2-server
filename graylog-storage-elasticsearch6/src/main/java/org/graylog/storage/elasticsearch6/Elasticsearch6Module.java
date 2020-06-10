package org.graylog.storage.elasticsearch6;

import org.graylog.events.indices.EventIndexerAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.plugin.PluginModule;

public class Elasticsearch6Module extends PluginModule {
    @Override
    protected void configure() {
        bind(IndicesAdapter.class).to(IndicesAdapterES6.class);
        bind(SearchesAdapter.class).to(SearchesAdapterES6.class);
        bind(MoreSearchAdapter.class).to(MoreSearchAdapterES6.class);
        bind(MessagesAdapter.class).to(MessagesAdapterES6.class);
        bind(ClusterAdapter.class).to(ClusterAdapterES6.class);
        bind(NodeAdapter.class).to(NodeAdapterES6.class);
        bind(EventIndexerAdapter.class).to(EventIndexerAdapterES6.class);
        bind(IndexFieldTypePollerAdapter.class).to(IndexFieldTypePollerAdapterES6.class);
    }
}
