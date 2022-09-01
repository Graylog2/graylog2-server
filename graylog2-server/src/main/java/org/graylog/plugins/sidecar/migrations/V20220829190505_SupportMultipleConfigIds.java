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
package org.graylog.plugins.sidecar.migrations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class V20220829190505_SupportMultipleConfigIds extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20220829190505_SupportMultipleConfigIds.class);

     private final ClusterConfigService clusterConfigService;

     private final MongoConnection mongoConnection;

    @Inject
    public V20220829190505_SupportMultipleConfigIds(ClusterConfigService clusterConfigService, MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-08-29T19:05:05Z");
    }

    @Override
    public void upgrade() {
        final MigrationCompleted migrationCompleted = clusterConfigService.get(MigrationCompleted.class);
        if (migrationCompleted != null) {
            return;
        }
        final MongoCollection<Document> sidecars = mongoConnection.getMongoDatabase().getCollection(SidecarService.COLLECTION_NAME);


        final AtomicLong count = new AtomicLong();
        sidecars.find(Filters.exists("configuration_id")).forEach((Consumer<? super Document>) sidecar -> {
                final Object assignments = sidecar.get("assignments");
                if (assignments instanceof List) {
                    //noinspection unchecked
                    ((List<Document>) assignments).forEach(a -> {
                        final Object configuration_id = a.get("configuration_id");
                        if (configuration_id != null) {
                            a.put("configuration_ids", ImmutableList.of(configuration_id));
                            a.remove("configuration_id");
                        }
                    });

                    final Document update = new Document();
                    update.put("$set", sidecar);
                    //sidecars.updateOne(new BasicDBObject("_id", sidecar.getObjectId("_id")), update);
                    sidecars.updateOne(Filters.eq("_id", sidecar.getObjectId("_id")), update);
                    count.getAndIncrement();
                }
            }
        );
        LOG.info("Migrated <{}> sidecars to support multiple configuration ids", count.get());

        clusterConfigService.write(MigrationCompleted.create());
    }

    @AutoValue
    public abstract static class MigrationCompleted {
        @JsonCreator
        public static MigrationCompleted create() {
            return new AutoValue_V20220829190505_SupportMultipleConfigIds_MigrationCompleted();
        }
    }
}
