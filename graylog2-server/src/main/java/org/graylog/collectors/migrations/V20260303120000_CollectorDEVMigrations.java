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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.FleetService;
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
import java.util.ArrayList;
import java.util.List;

import static org.graylog.collectors.CollectorsConfig.DEFAULT_HTTP_PORT;
import static org.graylog2.database.utils.MongoUtils.idEq;

/**
 * Migration for Collector changes during the 7.1 development.
 * Must be removed before 7.1.
 */
public class V20260303120000_CollectorDEVMigrations extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260303120000_CollectorDEVMigrations.class);
    private static final String CLUSTER_CONFIG_COLLECTION = "cluster_config";
    private static final String INPUTS_COLLECTION = "inputs";
    private static final String CONFIG_TYPE = "org.graylog.collectors.CollectorsConfig";
    private static final String INPUT_TYPE = "org.graylog.collectors.input.CollectorIngestHttpInput";

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

        migrateCollectorIngestConfig(db);

        removeTargetVersion(db);
        removeLegacyCollectorLogsStream();

        UserContext.<Void>runAs("admin", userServiceProvider.get(), () -> {
            logsDestinationServiceProvider.get().ensureExists();
            return null;
        });
    }

    /**
     * Migrates the collector ingest configuration:
     * <ol>
     *   <li>Strips the removed {@code enabled} and {@code input_id} fields from the persisted
     *       {@link org.graylog.collectors.CollectorsConfig} cluster config document.</li>
     *   <li>If {@code input_id} was present, updates that input with {@code bind_address} and {@code port}.</li>
     *   <li>Backfills missing transport config defaults ({@code recv_buffer_size}, {@code number_worker_threads},
     *       {@code idle_writer_timeout}, {@code tcp_keepalive}) on all persisted collector ingest inputs.</li>
     * </ol>
     */
    private void migrateCollectorIngestConfig(MongoDatabase db) {
        final MongoCollection<Document> clusterConfig = db.getCollection(CLUSTER_CONFIG_COLLECTION);
        final MongoCollection<Document> inputs = db.getCollection(INPUTS_COLLECTION);

        // 1. Clean the http sub-object in cluster config and migrate the linked input
        final Document configDoc = clusterConfig.find(Filters.eq("type", CONFIG_TYPE)).first();
        if (configDoc != null) {
            final Document payload = configDoc.get("payload", Document.class);
            final Document http = payload != null ? payload.get("http", Document.class) : null;

            if (http != null && (http.containsKey("input_id") || http.containsKey("enabled"))) {
                if (http.containsKey("input_id")) {
                    final String inputId = http.getString("input_id");
                    final int port = http.getInteger("port", DEFAULT_HTTP_PORT);
                    if (inputId != null && !inputId.isBlank()) {
                        final long modified = inputs.updateOne(
                                idEq(inputId),
                                Updates.combine(
                                        Updates.set("configuration.bind_address", "0.0.0.0"),
                                        Updates.set("configuration.port", port)
                                )
                        ).getModifiedCount();
                        if (modified > 0) {
                            LOG.info("Updated input <{}> configuration with bind_address=0.0.0.0 and port={}.", inputId, port);
                        }
                    }
                }

                final Document cleanHttp = new Document();
                cleanHttp.put("hostname", http.getString("hostname"));
                cleanHttp.put("port", http.getInteger("port", DEFAULT_HTTP_PORT));

                clusterConfig.updateOne(
                        Filters.eq("type", CONFIG_TYPE),
                        Updates.set("payload.http", cleanHttp)
                );
                LOG.info("Cleaned CollectorsConfig http sub-object: removed deprecated fields (enabled, input_id).");
            }
        }

        // 2. Backfill bind_address and port on all collector ingest inputs (previously runtime-injected, never persisted)
        for (final Document doc : inputs.find(Filters.eq("type", INPUT_TYPE))) {
            final Document config = doc.get("configuration", Document.class);
            if (config == null) {
                continue;
            }

            final List<Bson> needed = new ArrayList<>();
            if (!config.containsKey("bind_address")) {
                needed.add(Updates.set("configuration.bind_address", "0.0.0.0"));
            }
            if (!config.containsKey("port")) {
                needed.add(Updates.set("configuration.port", DEFAULT_HTTP_PORT));
            }

            if (!needed.isEmpty()) {
                inputs.updateOne(Filters.eq("_id", doc.getObjectId("_id")), Updates.combine(needed));
                LOG.info("Backfilled bind_address/port on collector ingest input <{}>.", doc.getObjectId("_id"));
            }
        }
    }

    private void removeTargetVersion(MongoDatabase db) {
        final MongoCollection<Document> fleetCollection = db.getCollection(FleetService.COLLECTION_NAME);
        final UpdateResult updateResult = fleetCollection.updateMany(Filters.empty(), Updates.unset("target_version"));
        LOG.info("Removed deprecated field `target_version` from {} fleets", updateResult.getModifiedCount());
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

        if ("Collector System Logs".equals(stream.getTitle())) {
            LOG.debug("Stream already migrated, nothing to do");
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
