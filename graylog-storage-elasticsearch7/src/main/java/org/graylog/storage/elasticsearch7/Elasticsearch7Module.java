package org.graylog.storage.elasticsearch7;

import com.google.inject.binder.LinkedBindingBuilder;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.plugin.VersionAwareModule;

import static org.graylog.storage.elasticsearch7.Elasticsearch7Plugin.SUPPORTED_ES_VERSION;

public class Elasticsearch7Module extends VersionAwareModule {
    @Override
    protected void configure() {
        bindForSupportedVersion(CountsAdapter.class).to(CountsAdapterES7.class);
        bindForSupportedVersion(ClusterAdapter.class).to(ClusterAdapterES7.class);
        bindForSupportedVersion(IndicesAdapter.class).to(IndicesAdapterES7.class);
        bindForSupportedVersion(MessagesAdapter.class).to(MessagesAdapterES7.class);
        bindForSupportedVersion(NodeAdapter.class).to(NodeAdapterES7.class);
    }

    private <T> LinkedBindingBuilder<T> bindForSupportedVersion(Class<T> interfaceClass) {
        return bindForVersion(SUPPORTED_ES_VERSION, interfaceClass);
    }
}
