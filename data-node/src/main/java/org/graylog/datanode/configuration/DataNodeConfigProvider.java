package org.graylog.datanode.configuration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.plugin.cluster.ClusterConfigService;

public class DataNodeConfigProvider implements Provider<DummyDNC> {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public DataNodeConfigProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public DummyDNC get() {
        final DataNodeConfig dataNodeConfig = clusterConfigService.get(DataNodeConfig.class);
        return new DummyDNC(dataNodeConfig.test());
    }
}
