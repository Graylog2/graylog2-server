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
package org.graylog.plugins.threatintel.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration;
import org.graylog.plugins.threatintel.adapters.otx.OTXDataAdapter;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.events.DataAdaptersUpdated;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class V20170821100300_MigrateOTXAPIToken extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20170821100300_MigrateOTXAPIToken.class);
    private static final ImmutableSet<String> OTX_DATA_ADAPTER_NAMES = ImmutableSet.of("otx-api-ip", "otx-api-domain");

    private final ClusterConfigService clusterConfigService;
    private final DBDataAdapterService dbDataAdapterService;
    private final ClusterEventBus clusterBus;

    @Inject
    public V20170821100300_MigrateOTXAPIToken(ClusterConfigService clusterConfigService,
                                              DBDataAdapterService dbDataAdapterService,
                                              ClusterEventBus clusterBus) {
        this.clusterConfigService = clusterConfigService;
        this.dbDataAdapterService = dbDataAdapterService;
        this.clusterBus = clusterBus;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2017-08-21T10:03:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final ThreatIntelPluginConfiguration pluginConfig = clusterConfigService.get(ThreatIntelPluginConfiguration.class);
        if (pluginConfig == null || isNullOrEmpty(pluginConfig.otxApiKey())) {
            LOG.debug("No existing OTX API key found, not running migration.");
            clusterConfigService.write(MigrationCompleted.notConvertedKey());
            return;
        }

        final String otxApiKey = pluginConfig.otxApiKey();

        final Set<String> updatedAdapterIds = OTX_DATA_ADAPTER_NAMES.stream()
                .map(name -> updateAdapter(name, otxApiKey))
                .collect(Collectors.toSet());

        clusterConfigService.write(MigrationCompleted.convertedKey(updatedAdapterIds));
    }

    private String updateAdapter(String name, String otxApiKey) {
        final DataAdapterDto dataAdapterDto = dbDataAdapterService.get(name)
                .orElseThrow(() -> new IllegalStateException("OTX data adapter <" + name + "> not present when trying to add API token."));

        final LookupDataAdapterConfiguration adapterConfig = dataAdapterDto.config();
        if (adapterConfig == null || !(adapterConfig instanceof OTXDataAdapter.Config)) {
            throw new IllegalStateException("OTX Data Adapter <" + name + "> does not contain config or config has wrong type.");
        }

        final OTXDataAdapter.Config config = (OTXDataAdapter.Config) adapterConfig;

        final DataAdapterDto newDto = DataAdapterDto.builder()
                .id(dataAdapterDto.id())
                .config(config.toBuilder().apiKey(otxApiKey).build())
                .title(dataAdapterDto.title())
                .description(dataAdapterDto.description())
                .name(dataAdapterDto.name())
                .contentPack(dataAdapterDto.contentPack())
                .build();

        final DataAdapterDto saved = dbDataAdapterService.save(newDto);

        clusterBus.post(DataAdaptersUpdated.create(saved.id()));

        return saved.id();
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("converted_otx_api_key")
        public abstract boolean convertedOTXAPIKey();

        @JsonProperty("data_adapter_ids")
        public abstract Set<String> dataAdapterIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("data_adapter_ids") final Set<String> dataAdapterIds,
                                                @JsonProperty("converted_otx_api_key") final boolean convertedOTXAPIKey) {
            return new AutoValue_V20170821100300_MigrateOTXAPIToken_MigrationCompleted(convertedOTXAPIKey, dataAdapterIds);
        }

        public static MigrationCompleted convertedKey(@JsonProperty("data_adapter_ids") final Set<String> dataAdapterIds) {
            return create(dataAdapterIds, true);
        }

        public static MigrationCompleted notConvertedKey() {
            return create(Collections.emptySet(), false);
        }
    }
}
