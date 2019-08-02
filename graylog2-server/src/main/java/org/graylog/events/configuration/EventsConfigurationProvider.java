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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotNull;
import java.util.Optional;

public class EventsConfigurationProvider implements Provider<EventsConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(EventsConfigurationProvider.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public EventsConfigurationProvider(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public EventsConfiguration get() {
        return loadFromDatabase().orElse(getDefaultConfig());
    }

    public EventsConfiguration getDefaultConfig() {
        return EventsConfiguration.builder().build();
    }

    @NotNull
    public Optional<EventsConfiguration> loadFromDatabase() {
        try {
            return Optional.ofNullable(clusterConfigService.get(EventsConfiguration.class));
        } catch (Exception e) {
            LOG.error("Failed to fetch events configuration from database", e);
            return Optional.empty();
        }
    }
}
