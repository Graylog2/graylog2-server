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
import jakarta.inject.Inject;
import org.graylog.plugins.threatintel.adapters.otx.OTXDataAdapter;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.system.urlallowlist.LiteralAllowlistEntry;
import org.graylog2.system.urlallowlist.UrlAllowlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Adds the OTX default API URL ({@value OTXDataAdapter#DEFAULT_API_URL}) to the URL allowlist for any
 * existing OTX adapter instance that is still configured with that default value.
 *
 * <p>This migration is required because Graylog 7.2 introduces mandatory URL allowlist enforcement for the
 * OTX lookup data adapter. Existing instances using the default URL will automatically have it added to the
 * allowlist. Instances with a custom {@code api_url} must be manually added to the allowlist by an
 * administrator.</p>
 */
public class V20260827120000_AddOtxDefaultUrlToAllowlist extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260827120000_AddOtxDefaultUrlToAllowlist.class);

    private final ClusterConfigService clusterConfigService;
    private final DBDataAdapterService dbDataAdapterService;
    private final UrlAllowlistService urlAllowlistService;

    @Inject
    public V20260827120000_AddOtxDefaultUrlToAllowlist(ClusterConfigService clusterConfigService,
                                                        DBDataAdapterService dbDataAdapterService,
                                                        UrlAllowlistService urlAllowlistService) {
        this.clusterConfigService = clusterConfigService;
        this.dbDataAdapterService = dbDataAdapterService;
        this.urlAllowlistService = urlAllowlistService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-08-27T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        LOG.info("Running migration: checking for OTX adapter instances using the default API URL <{}>.",
                OTXDataAdapter.DEFAULT_API_URL);

        int addedCount = 0;

        try (final Stream<DataAdapterDto> stream = dbDataAdapterService.streamAll()) {
            final boolean hasDefaultUrlAdapter = stream
                    .map(DataAdapterDto::config)
                    .filter(OTXDataAdapter.Config.class::isInstance)
                    .map(OTXDataAdapter.Config.class::cast)
                    .anyMatch(config -> OTXDataAdapter.DEFAULT_API_URL.equals(config.apiUrl()));

            if (!hasDefaultUrlAdapter) {
                LOG.info("No OTX adapter instances with the default API URL found. Nothing to migrate.");
            } else if (urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)) {
                LOG.info("OTX default API URL <{}> is already present in the URL allowlist. No entry added.",
                        OTXDataAdapter.DEFAULT_API_URL);
            } else {
                urlAllowlistService.addEntry(LiteralAllowlistEntry.create(
                        UUID.randomUUID().toString(),
                        "AlienVault OTX threat intel API (auto-migrated)",
                        OTXDataAdapter.DEFAULT_API_URL
                ));
                addedCount = 1;
                LOG.info("Added OTX default API URL <{}> to the URL allowlist.", OTXDataAdapter.DEFAULT_API_URL);
            }
        }

        clusterConfigService.write(MigrationCompleted.create(addedCount));
        LOG.info("Migration complete. Allowlist entries added: {}.", addedCount);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("added_allowlist_entry_count")
        public abstract int addedAllowlistEntryCount();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("added_allowlist_entry_count") int addedAllowlistEntryCount) {
            return new AutoValue_V20260827120000_AddOtxDefaultUrlToAllowlist_MigrationCompleted(addedAllowlistEntryCount);
        }
    }
}
