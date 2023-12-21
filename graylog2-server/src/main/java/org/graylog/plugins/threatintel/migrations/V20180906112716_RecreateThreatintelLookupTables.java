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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class V20180906112716_RecreateThreatintelLookupTables extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20180906112716_RecreateThreatintelLookupTables.class);

    private final ObjectMapper objectMapper;
    private final ClusterConfigService clusterConfigService;
    private final ContentPackPersistenceService contentPackPersistenceService;

    @Inject
    public V20180906112716_RecreateThreatintelLookupTables(final ContentPackPersistenceService contentPackPersistenceService,
                                                           final ObjectMapper objectMapper,
                                                           final ClusterConfigService clusterConfigService) {
        this.objectMapper = objectMapper;
        this.clusterConfigService = clusterConfigService;
        this.contentPackPersistenceService = contentPackPersistenceService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-09-06T11:27:16Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        try {
            final String[] contentPacks = {
                    "V20180906112716_RecreateThreatintelLookupTables-content_pack-OTX.json",
                    "V20180906112716_RecreateThreatintelLookupTables-content_pack-tor.json",
                    "V20180906112716_RecreateThreatintelLookupTables-content_pack-abuse.json",
                    "V20180906112716_RecreateThreatintelLookupTables-content_pack-spamhaus.json",
                    "V20180906112716_RecreateThreatintelLookupTables-content_pack-whois.json",
            };

            List<String> newContentPackIds = new ArrayList<>();
            for (String contentPackName : contentPacks) {
                final URL contentPackURL;
                final ContentPack contentPack;
                contentPackURL = V20180906112716_RecreateThreatintelLookupTables.class.getResource(contentPackName);
                contentPack = this.objectMapper.readValue(contentPackURL, ContentPack.class);
                ContentPack pack = this.contentPackPersistenceService.insert(contentPack)
                        .orElseThrow(() -> {
                            clusterConfigService.write(MigrationCompleted.create(newContentPackIds));
                            return new ContentPackException("Content pack " + contentPack.id() + " with this revision " + contentPack.revision() + " already found!");
                        });
                newContentPackIds.add(pack.id().toString());
            }

            clusterConfigService.write(MigrationCompleted.create(newContentPackIds));
        } catch (IOException e) {
            LOG.error("Unable to import content pack for threat intel plugin: ", e);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("content_bundle_ids")
        public abstract List<String> contentBundleIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("content_bundle_ids") final List<String> contentBundleIds) {
            return new AutoValue_V20180906112716_RecreateThreatintelLookupTables_MigrationCompleted(contentBundleIds);
        }
    }
}
