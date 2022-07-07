package org.graylog2.configuration;

import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V202207061200_CreateDefaultIndexDefaultsConfig extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V202207061200_CreateDefaultIndexDefaultsConfig.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public V202207061200_CreateDefaultIndexDefaultsConfig(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        // this migration should run early
        return ZonedDateTime.parse("2022-07-06T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(IndexDefaultsConfiguration.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        try {
            clusterConfigService.write(IndexDefaultsConfiguration.createNew());
            LOG.debug("Index defaults config saved.");
        } catch (Exception e) {
            LOG.error("Unable to write index defaults configuration.", e);
        }
    }
}
