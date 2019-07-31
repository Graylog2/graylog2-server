/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
