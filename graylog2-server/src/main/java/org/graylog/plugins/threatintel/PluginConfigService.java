/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.threatintel;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.events.DataAdaptersUpdated;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Provides up to date access to this plugins' cluster config without forcing consumers to listen to updates manually.
 */
@Singleton
public class PluginConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(PluginConfigService.class);

    private final ClusterConfigService clusterConfigService;
    private final DBDataAdapterService dbDataAdapterService;
    private final ClusterEventBus clusterEventBus;
    private AtomicReference<ConfigVersions<ThreatIntelPluginConfiguration>> config = new AtomicReference<>();

    @Inject
    public PluginConfigService(final ClusterConfigService clusterConfigService,
                               final EventBus serverEventBus,
                               final DBDataAdapterService dbDataAdapterService,
                               final ClusterEventBus clusterEventBus) {
        this.clusterConfigService = clusterConfigService;
        this.dbDataAdapterService = dbDataAdapterService;
        this.clusterEventBus = clusterEventBus;
        final ThreatIntelPluginConfiguration currentVersion = clusterConfigService.get(ThreatIntelPluginConfiguration.class);
        final ConfigVersions<ThreatIntelPluginConfiguration> versions = ConfigVersions.of(null,
                Optional.ofNullable(currentVersion).orElse(ThreatIntelPluginConfiguration.defaults()));
        config.set(versions);
        serverEventBus.register(this);
    }

    public ConfigVersions<ThreatIntelPluginConfiguration> config() {
        return config.get();
    }

    @Subscribe
    public void handleUpdatedClusterConfig(ClusterConfigChangedEvent clusterConfigChangedEvent) {
        if (clusterConfigChangedEvent.type().equals(AutoValueUtils.getCanonicalName((ThreatIntelPluginConfiguration.class)))) {
            final ThreatIntelPluginConfiguration currentVersion = Optional.ofNullable(clusterConfigService.get(ThreatIntelPluginConfiguration.class))
                    .orElse(ThreatIntelPluginConfiguration.defaults());
            final ThreatIntelPluginConfiguration previous = config.get().getCurrent();
            config.set(ConfigVersions.of(previous, currentVersion));

            // check for changes in the configuration and bounce the corresponding adapters if something changed
            final ImmutableList.Builder<Object> adaptersToLoad = ImmutableList.builder();
            if (previous.abusechRansomEnabled() != currentVersion.abusechRansomEnabled()) {
                adaptersToLoad.add("abuse-ch-ransomware-domains", "abuse-ch-ransomware-ip");
            }
            if (previous.torEnabled() != currentVersion.torEnabled()) {
                adaptersToLoad.add("tor-exit-node");
            }
            if (previous.spamhausEnabled() != currentVersion.spamhausEnabled()) {
                adaptersToLoad.add("spamhaus-drop");
            }

            // we request 10 per page, which is more than enough for the 4 that we potentially have to bounce
            final ImmutableList<Object> adapterNames = adaptersToLoad.build();
            final PaginatedList<DataAdapterDto> adapterDtos = dbDataAdapterService.findPaginated(
                    DBQuery.in(DataAdapterDto.FIELD_NAME, adapterNames),
                    DBSort.asc(DataAdapterDto.FIELD_ID),
                    1,
                    10);

            final Set<String> adapterIds = adapterDtos.delegate().stream().map(DataAdapterDto::id).collect(Collectors.toSet());
            if (!adapterIds.isEmpty()) {
                LOG.debug("Restarting data adapters {} due to updated enabled/disabled states", adapterNames);
                // this takes care of restarting the lookup tables and necessary adapters and caches to reflect their "enabled-ness"
                clusterEventBus.post(DataAdaptersUpdated.create(adapterIds));
            }
        }
    }

    /**
     * Used by {@link PluginConfigService} to return the previously observed and current configuration
     * so that clients can act on changes if they need to.
     *
     * @param <T> the plugin cluster configuration class
     */
    public static class ConfigVersions<T> {

        @Nullable
        private final T previous;

        @Nonnull
        private final T current;

        public ConfigVersions(@Nullable T previous, @Nonnull T current) {
            this.previous = previous;
            this.current = current;
        }

        public static <T> ConfigVersions<T> of(@Nullable T previous, @Nonnull T current) {
            return new ConfigVersions<>(previous, current);
        }

        public Optional<T> getPrevious() {
            return Optional.ofNullable(previous);
        }

        @Nonnull
        public T getCurrent() {
            return current;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigVersions<?> that = (ConfigVersions<?>) o;
            return Objects.equals(previous, that.previous) &&
                    Objects.equals(current, that.current);
        }

        @Override
        public int hashCode() {
            return Objects.hash(previous, current);
        }
    }
}
