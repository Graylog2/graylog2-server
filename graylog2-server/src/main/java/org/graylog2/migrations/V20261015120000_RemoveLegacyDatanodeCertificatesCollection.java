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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Drops the {@code data_node_certificates} collection. Data node keystores used to be persisted there,
 * but the legacy MongoDB keystore provider has been removed and the collection is no longer read.
 */
public class V20261015120000_RemoveLegacyDatanodeCertificatesCollection extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20261015120000_RemoveLegacyDatanodeCertificatesCollection.class);

    static final String LEGACY_COLLECTION_NAME = "data_node_certificates";

    private final MongoConnection mongoConnection;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20261015120000_RemoveLegacyDatanodeCertificatesCollection(MongoConnection mongoConnection,
                                                                     ClusterConfigService clusterConfigService) {
        this.mongoConnection = mongoConnection;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-10-15T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        LOG.info("Dropping legacy data node keystore collection <{}>", LEGACY_COLLECTION_NAME);
        mongoConnection.getMongoDatabase().getCollection(LEGACY_COLLECTION_NAME).drop();

        clusterConfigService.write(new MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
