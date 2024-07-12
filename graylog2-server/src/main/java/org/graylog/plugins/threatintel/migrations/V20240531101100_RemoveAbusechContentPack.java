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

import jakarta.inject.Inject;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Set;

public class V20240531101100_RemoveAbusechContentPack extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20240531101100_RemoveAbusechContentPack.class);
    private static final ModelId CONTENT_PACK_ID = ModelId.of("cbacd801-7824-c554-3fb1-475491b03826");

    private final ContentPackService contentPackService;
    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20240531101100_RemoveAbusechContentPack(final ContentPackService contentPackService,
                                                    final ContentPackPersistenceService contentPackPersistenceService,
                                                    final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                                                    final ClusterConfigService clusterConfigService) {
        this.contentPackService = contentPackService;
        this.contentPackPersistenceService = contentPackPersistenceService;
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-05-31T10:11:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20240531101100_RemoveAbusechContentPack.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }
        Set<ContentPack> existingPacks = contentPackPersistenceService.findAllById(CONTENT_PACK_ID);
        if (!existingPacks.isEmpty()) {
            LOG.debug("Removing deprecated Abuse.ch content pack");
            existingPacks.forEach(pack -> {
                contentPackInstallationPersistenceService.findByContentPackIdAndRevision(CONTENT_PACK_ID, pack.revision())
                        .forEach(i -> contentPackService.uninstallContentPack(pack, i));
                contentPackPersistenceService.deleteById(pack.id());
            });
        }
        clusterConfigService.write(new MigrationCompleted());
    }

    record MigrationCompleted() {}
}
