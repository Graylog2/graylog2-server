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
package org.graylog.events.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import jakarta.inject.Inject;
import org.bson.Document;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.utilities.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class V20230629140000_RenameFieldTypeOfEventDefinitionSeries extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230629140000_RenameFieldTypeOfEventDefinitionSeries.class);
    private static final String SERIES_PATH_STRING = "config.series";
    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> collection;
    private final NotificationService notificationService;


    @Inject
    public V20230629140000_RenameFieldTypeOfEventDefinitionSeries(ClusterConfigService clusterConfigService,
                                                                  MongoConnection mongoConnection,
                                                                  NotificationService notificationService) {
        this.clusterConfigService = clusterConfigService;
        this.collection = mongoConnection.getMongoDatabase().getCollection("event_definitions");
        this.notificationService = notificationService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-06-29T14:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        var result = collection.find(Filters.and(
                Filters.eq("config.type", "aggregation-v1"),
                Filters.type(SERIES_PATH_STRING, "array"),
                Filters.exists(SERIES_PATH_STRING + ".0")
        ));

        var bulkOperations = new ArrayList<WriteModel<? extends Document>>();
        for (Document doc : result) {
            processDoc(doc, bulkOperations);
        }
        if (bulkOperations.size() > 0) {
            collection.bulkWrite(bulkOperations);
        }
        this.clusterConfigService.write(new MigrationCompleted());
    }

    private void processDoc(Document doc, List<WriteModel<? extends Document>> bulkOperations) {
        var config = doc.get("config", Document.class);
        var series = config.getList("series", Document.class, Collections.emptyList());
        var needsUpdate = false;
        var invalidSeries = false;
        var newSeries = new ArrayList<Document>(series.size());

        for (var s : series) {
            if (invalidSeries) {
                break;
            }
            if (!s.containsKey("function")) {
                newSeries.add(s);
            } else {
                needsUpdate = true;
                s.put("type", s.get("function"));
                s.remove("function");

                if (s.containsKey("field") && s.get("field") == null
                        && (s.get("type").equals("card") || s.get("type").equals("max") || s.get("type").equals("percentile"))) {
                    invalidSeries = true;
                } else {
                    newSeries.add(s);
                }
            }
        }
        if (invalidSeries) {
            newSeries.clear();
            Document newConditions = new Document();
            newConditions.put("expression", null);
            config.put("conditions", newConditions);
            doc.put(EventDefinitionDto.FIELD_STATE, EventDefinition.State.DISABLED);
            raiseNotification(StringUtils.f("Disabled invalid event definition %s", doc.get("title", String.class)),
                    "Definition is missing the required field name - please review and update the definition.",
                    "/alerts/definitions/" + doc.getObjectId("_id"));
        }
        if (needsUpdate) {
            config.put("series", newSeries);
            doc.put("config", config);
            bulkOperations.add(new ReplaceOneModel<>(Filters.eq("_id", doc.getObjectId("_id")), doc, new ReplaceOptions().upsert(false)));
        }
    }

    public record MigrationCompleted() {}

    private void raiseNotification(String title, String details, String url) {
        final Notification systemNotification = notificationService.buildNow()
                .addType(Notification.Type.GENERIC_WITH_LINK)
                .addSeverity(Notification.Severity.URGENT)
                .addDetail("title", title)
                .addDetail("GENERIC_DETAILS", details)
                .addDetail("GENERIC_URL", url);
        notificationService.publishIfFirst(systemNotification);
    }
}
