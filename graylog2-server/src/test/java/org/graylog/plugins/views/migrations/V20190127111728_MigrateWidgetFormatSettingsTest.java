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

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class V20190127111728_MigrateWidgetFormatSettingsTest {
    @Rule
    public final MongoDBInstance mongoDB = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20190127111728_MigrateWidgetFormatSettings migration;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() {
        migration = new V20190127111728_MigrateWidgetFormatSettings(mongoDB.mongoConnection(), clusterConfigService);
    }

    @Test
    @MongoDBFixtures("V20190127111728_MigrateWidgetFormatSettings.json")
    public void testMigrationWithOneChartColorMapping() {
        final BasicDBObject dbQuery1 = new BasicDBObject();
        dbQuery1.put("_id", new ObjectId("5e2ee372b22d7970576b2eb3"));
        final MongoCollection<Document> collection = mongoDB.mongoConnection()
                .getMongoDatabase()
                .getCollection("views");
        migration.upgrade();
        final FindIterable<Document> views = collection.find(dbQuery1);
        final Document view1 = views.first();

        @SuppressWarnings("unchecked")
        final List<Document> widgets1 = (List) view1.get("state", Document.class).get("2c67cc0f-c62e-47c1-8b70-e3198925e6bc", Document.class).get("widgets");
        assertThat(widgets1.size()).isEqualTo(2);
        Set<Document> aggregationWidgets =widgets1.stream().filter(w -> w.getString("type")
                .equals("aggregation")).collect(Collectors.toSet());
        assertThat(aggregationWidgets.size()).isEqualTo(1);
        final Document aggregationWidget = aggregationWidgets.iterator().next();
        final Document config = aggregationWidget.get("config", Document.class);
        final Document formattingSettings = config.get("formatting_settings", Document.class);

        @SuppressWarnings("unchecked")
        final List<Document> chartColors = (List) formattingSettings.get("chart_colors", List.class);
        assertThat(chartColors.size()).isEqualTo(1);
        final Document chartColor = chartColors.get(0);
        assertThat(chartColor.getString("field_name")).isEqualTo("count()");
        assertThat(chartColor.getString("chart_color")).isEqualTo("#e91e63");
    }

    @Test
    @MongoDBFixtures("V20190127111728_MigrateWidgetFormatSettings_without_color_mapping.json")
    public void testMigrationWithoutChartColorMapping() {
        final BasicDBObject dbQuery1 = new BasicDBObject();
        dbQuery1.put("_id", new ObjectId("5e2ee372b22d7970576b2eb3"));
        final MongoCollection<Document> collection = mongoDB.mongoConnection()
                .getMongoDatabase()
                .getCollection("views");
        migration.upgrade();
        final FindIterable<Document> views = collection.find(dbQuery1);
        final Document view1 = views.first();

        @SuppressWarnings("unchecked")
        final List<Document> widgets1 = (List) view1.get("state", Document.class).get("2c67cc0f-c62e-47c1-8b70-e3198925e6bc", Document.class).get("widgets");
        assertThat(widgets1.size()).isEqualTo(2);
        Set<Document> aggregationWidgets =widgets1.stream().filter(w -> w.getString("type")
                .equals("aggregation")).collect(Collectors.toSet());
        assertThat(aggregationWidgets.size()).isEqualTo(1);
        final Document aggregationWidget = aggregationWidgets.iterator().next();
        final Document config = aggregationWidget.get("config", Document.class);
        final Document formattingSettings = config.get("formatting_settings", Document.class);

        assertThat(formattingSettings.get("chart_colors")).isNull();
    }

    @Test
    @MongoDBFixtures("V20190127111728_MigrateWidgetFormatSettings_withMultipleColorMappings.json")
    public void testMigrationWithMultipleChartColorMapping() {
        final BasicDBObject dbQuery1 = new BasicDBObject();
        dbQuery1.put("_id", new ObjectId("5e2ee372b22d7970576b2eb3"));
        final MongoCollection<Document> collection = mongoDB.mongoConnection()
                .getMongoDatabase()
                .getCollection("views");
        migration.upgrade();
        final FindIterable<Document> views = collection.find(dbQuery1);
        final Document view1 = views.first();

        @SuppressWarnings("unchecked")
        final List<Document> widgets1 = (List) view1.get("state", Document.class).get("2c67cc0f-c62e-47c1-8b70-e3198925e6bc", Document.class).get("widgets");
        assertThat(widgets1.size()).isEqualTo(2);
        Set<Document> aggregationWidgets =widgets1.stream().filter(w -> w.getString("type")
                .equals("aggregation")).collect(Collectors.toSet());
        assertThat(aggregationWidgets.size()).isEqualTo(1);
        final Document aggregationWidget = aggregationWidgets.iterator().next();
        final Document config = aggregationWidget.get("config", Document.class);
        final Document formattingSettings = config.get("formatting_settings", Document.class);

        @SuppressWarnings("unchecked")
        final List<Document> chartColors = (List) formattingSettings.get("chart_colors", List.class);
        assertThat(chartColors.size()).isEqualTo(4);
        final Document chartColor1 = chartColors.get(0);
        assertThat(chartColor1.getString("field_name")).isEqualTo("count()");
        assertThat(chartColor1.getString("chart_color")).isEqualTo("#e91e63");

        final Document chartColor2 = chartColors.get(1);
        assertThat(chartColor2.getString("field_name")).isEqualTo("avg(fields)");
        assertThat(chartColor2.getString("chart_color")).isEqualTo("#e81e63");

        final Document chartColor3 = chartColors.get(2);
        assertThat(chartColor3.getString("field_name")).isEqualTo("mean(man)");
        assertThat(chartColor3.getString("chart_color")).isEqualTo("#e91f63");

        final Document chartColor4 = chartColors.get(3);
        assertThat(chartColor4.getString("field_name")).isEqualTo("total(win)");
        assertThat(chartColor4.getString("chart_color")).isEqualTo("#e91fff");
    }
}
