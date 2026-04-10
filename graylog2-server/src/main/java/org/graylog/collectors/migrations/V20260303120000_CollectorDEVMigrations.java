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
package org.graylog.collectors.migrations;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.security.UserContext;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.jobs.IndexSetCleanupJob;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamGuardException;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Migration for Collector changes during the 7.1 development.
 * Must be removed before 7.1.
 */
public class V20260303120000_CollectorDEVMigrations extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260303120000_CollectorDEVMigrations.class);

    private final MongoConnection mongoConnection;
    private final Provider<StreamService> streamServiceProvider;
    private final Provider<IndexSetService> indexSetServiceProvider;
    private final Provider<IndexSetCleanupJob.Factory> indexSetCleanupJobFactoryProvider;
    private final Provider<UserService> userServiceProvider;
    private final Provider<CollectorLogsDestinationService> logsDestinationServiceProvider;

    @Inject
    public V20260303120000_CollectorDEVMigrations(MongoConnection mongoConnection,
                                                  Provider<StreamService> streamServiceProvider,
                                                  Provider<IndexSetService> indexSetServiceProvider,
                                                  Provider<IndexSetCleanupJob.Factory> indexSetCleanupJobFactoryProvider,
                                                  Provider<UserService> userServiceProvider,
                                                  Provider<CollectorLogsDestinationService> logsDestinationServiceProvider) {
        this.mongoConnection = mongoConnection;
        this.streamServiceProvider = streamServiceProvider;
        this.indexSetServiceProvider = indexSetServiceProvider;
        this.indexSetCleanupJobFactoryProvider = indexSetCleanupJobFactoryProvider;
        this.userServiceProvider = userServiceProvider;
        this.logsDestinationServiceProvider = logsDestinationServiceProvider;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-03-03T12:00:00Z");
    }

    @Override
    public void upgrade() {
        LOG.warn("This migration MUST be removed before the 7.1 GA release!");

        final var db = mongoConnection.getMongoDatabase();

        removeLegacyCollectorLogsStream();

        UserContext.<Void>runAs("admin", userServiceProvider.get(), () -> {
            logsDestinationServiceProvider.get().ensureExists();
            return null;
        });
    }

    private void removeLegacyCollectorLogsStream() {
        final var streamService = streamServiceProvider.get();

        final Stream stream;
        try {
            stream = streamService.load(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID);
        } catch (NotFoundException e) {
            LOG.debug("Stream not found, nothing to do");
            return;
        }

        LOG.info("Removing legacy Collector Logs stream and index-set");

        final var indexSet = stream.getIndexSet();

        try {
            streamService.destroy(stream);
        } catch (NotFoundException | StreamGuardException e) {
            throw new RuntimeException("Couldn't destroy stream", e);
        }

        if (indexSetServiceProvider.get().delete(indexSet.getConfig().id()) > 0) {
            indexSetCleanupJobFactoryProvider.get().create(indexSet).execute();
        }
    }
}
