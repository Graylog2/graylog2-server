package org.graylog.storage.elasticsearch6;

import org.graylog.events.search.MoreSearchAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.plugin.PluginModule;

public class Elasticsearch6Module extends PluginModule {
    @Override
    protected void configure() {
        bind(SearchesAdapter.class).to(SearchesAdapterES6.class);
        bind(MoreSearchAdapter.class).to(MoreSearchAdapterES6.class);
    }
}
