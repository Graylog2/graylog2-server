package org.graylog.datanode.configuration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public class DataNodeConfigProvider implements Provider<DataNodeConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeConfigProvider.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public DataNodeConfigProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public DataNodeConfig get() {
        return loadFromDatabase().orElse(getDefaultConfig());
    }

    public DataNodeConfig getDefaultConfig() {
        return new DataNodeConfig(null);
    }

    @NotNull
    public Optional<DataNodeConfig> loadFromDatabase() {
        try {
            return Optional.ofNullable(clusterConfigService.get(DataNodeConfig.class));
        } catch (Exception e) {
            LOG.error("Failed to fetch datanode configuration from database", e);
            return Optional.empty();
        }
    }

}
