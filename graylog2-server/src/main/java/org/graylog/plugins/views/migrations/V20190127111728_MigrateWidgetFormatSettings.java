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
package org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import com.google.protobuf.MapEntry;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class V20190127111728_MigrateWidgetFormatSettings extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190127111728_MigrateWidgetFormatSettings.class);

    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> viewsCollection;

    @Inject
    public V20190127111728_MigrateWidgetFormatSettings(MongoConnection mongoConnection,
                                                       ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.viewsCollection = mongoConnection.getMongoDatabase().getCollection("views");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-01-27T11:17:28Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final Set<String> viewIds = new HashSet<>();
        final FindIterable<Document> documents = viewsCollection.find();
        boolean viewMigrated;
        for (final Document view : documents) {
            viewMigrated = false;
            final Document states = view.get("state", Document.class);
            for(Map.Entry<String, Object> obj : states.entrySet()) {
                final Document state = (Document) obj.getValue();
                if (state.get("widgets") instanceof List) {
                    @SuppressWarnings("unchecked") final List<Document> widgets = (List) state.get("widgets");
                    for (final Document widget : widgets) {
                        final String type = widget.getString("type");
                        if (type.equals("aggregation")) {
                            final Document config = widget.get("config", Document.class);
                            final Document formatSettings = config.get("formatting_settings", Document.class);
                            if (formatSettings == null) {
                                continue;
                            }
                            final Object charColorsObj = formatSettings.get("chart_colors");
                            if (charColorsObj == null) {
                                continue;
                            }
                            viewMigrated = true;
                            @SuppressWarnings({"unchecked", "rawtypes"}) final Map<String, String> chartColors =
                                    (Map) charColorsObj;
                            List<Document> chartColorSettings = chartColors.entrySet().stream().map(entry -> {
                                final Document chartColorFieldSetting = new Document();
                                chartColorFieldSetting.put("field_name", entry.getKey());
                                chartColorFieldSetting.put("chart_color", entry.getValue());
                                return chartColorFieldSetting;
                            }).collect(Collectors.toList());
                            formatSettings.put("chart_colors", chartColorSettings);
                            config.put("formatting_settings", formatSettings);
                            widget.put("config", config);
                        }
                    }
                    if (viewMigrated) {
                        state.put("widgets", widgets);
                    }
                }
            }

            if (viewMigrated) {
                viewsCollection.updateOne(new BasicDBObject("_id", view.getObjectId("_id")), new Document("$set", view));
                final String viewId = view.getObjectId("_id").toString();
                viewIds.add(viewId);
            }
        }
        LOG.info("Migration completed. {} views where migrated.", viewIds.size());
        clusterConfigService.write(V20190127111728_MigrateWidgetFormatSettings.MigrationCompleted.create(
                viewIds.size(), viewIds));
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("modified_views_count")
        public abstract long modifiedViewsCount();

        @JsonProperty("modified_view_ids")
        public abstract Set<String> modifiedViewIds();

        @JsonCreator
        public static V20190127111728_MigrateWidgetFormatSettings.MigrationCompleted create(
                @JsonProperty("modified_views_count") final long modifiedViews,
                @JsonProperty("modified_view_ids") final Set<String> modifiedViewIds) {
            return new AutoValue_V20190127111728_MigrateWidgetFormatSettings_MigrationCompleted(
                    modifiedViews, modifiedViewIds);
        }
    }
}
