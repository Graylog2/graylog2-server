package org.graylog.events.configuration;

import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotNull;
import java.util.Optional;

public class EventsConfigurationProvider implements Provider<EventsConfiguration> {
private static final EventsConfiguration DEFAULT_CONFIG = EventsConfiguration.builder().build();

    private final ClusterConfigService clusterConfigService;

    @Inject
    public EventsConfigurationProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public EventsConfiguration get() {
        return getEventsConfig().orElse(DEFAULT_CONFIG);
    }

    @NotNull
    public Optional<EventsConfiguration> getEventsConfig() {
        final EventsConfiguration dto = clusterConfigService.get(EventsConfiguration.class);

        return dto != null ? Optional.of(dto) : Optional.empty();
    }
}
