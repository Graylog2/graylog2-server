package org.graylog2.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.database.MongoConnection;

import java.net.URI;
import java.util.List;

public class IndexDiscoveryModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<List<URI>>() {}).annotatedWith(IndexerHosts.class).toProvider(IndexerDiscoveryProvider.class).asEagerSingleton();
        bind(NodeService.class).to(NodeServiceImpl.class);
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
    }
}
