package org.graylog2.migrations;

import org.graylog.events.configuration.EventsConfiguration;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Optional;

public class V20190730000000_CreateDefaultEventsConfiguration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190730000000_CreateDefaultEventsConfiguration.class);

    private final EventsConfigurationProvider configProvider;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20190730000000_CreateDefaultEventsConfiguration(EventsConfigurationProvider configProvider,
                                                            ClusterConfigService clusterConfigService) {
        this.configProvider = configProvider;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-07-30T00:00:00Z");
    }

    @Override
    public void upgrade() {
        final Optional<EventsConfiguration> config = configProvider.getEventsConfig();
        if (config.isPresent()) {
            LOG.debug("Found events configuration, no migration necessary.");
            return;
        }
        try {
            final EventsConfiguration defaultConfig = configProvider.get();
            clusterConfigService.write(defaultConfig);
            LOG.debug("Create default events configuration: {}", defaultConfig);
        } catch (Exception e) {
            LOG.error("Unable to write default events configuration", e);
        }
    }
}
