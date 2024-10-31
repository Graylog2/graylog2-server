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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
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

import static org.assertj.core.api.Assertions.assertThat;

public class V20200102140000_UnifyEventSeriesIdTestIT {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20200102140000_UnifyEventSeriesId migration;

    @Mock
    private ClusterConfigService clusterConfigService;
    private MongoCollection<Document> eventDefinitions;

    @Before
    public void setUp() throws Exception {
        migration = new V20200102140000_UnifyEventSeriesId(clusterConfigService, mongodb.mongoConnection());

        this.eventDefinitions = mongodb.mongoConnection().getMongoDatabase().getCollection("event_definitions");
    }

    @Test
    @MongoDBFixtures("V20200102140000_UnifyEventSeriesIdTestIT.json")
    public void testMigration() {
        assertThat(this.eventDefinitions.countDocuments()).isEqualTo(2);
        var eventDefinitionBefore = this.eventDefinitions.find(Filters.eq("_id", new ObjectId("58458e442f857c314491344e"))).first();
        var config = eventDefinitionBefore.get("config", Document.class);
        assertThat(config.getString("type")).isEqualTo(AggregationEventProcessorConfig.TYPE_NAME);
        assertThat(config.getList("series", Document.class).get(0).getString("id")).isEqualTo("4711-2342");
        assertThat(config.getEmbedded(List.of("conditions", "expression", "left", "ref"), String.class)).isEqualTo("4711-2342");
        assertThat(config.getEmbedded(List.of("conditions", "expression", "right", "value"), Double.class)).isEqualTo(3.0);

        migration.upgrade();

        assertThat(eventDefinitions.countDocuments()).isEqualTo(2);

        var eventDefinition1 = eventDefinitions.find(Filters.eq("_id", new ObjectId("58458e442f857c314491344e"))).first();
        var config1 = eventDefinition1.get("config", Document.class);
        assertThat(config1.getString("type")).isEqualTo(AggregationEventProcessorConfig.TYPE_NAME);
        assertThat(config1.getList("series", Document.class).get(0).getString("id")).isEqualTo("max-login_count");
        assertThat(config1.getEmbedded(List.of("conditions", "expression", "left", "ref"), String.class)).isEqualTo("max-login_count");
        assertThat(config1.getEmbedded(List.of("conditions", "expression", "right", "value"), Double.class)).isEqualTo(3.0);

        var eventDefinition2 = eventDefinitions.find(Filters.eq("_id", new ObjectId("5d3af98fdc820b587bc354bc"))).first();
        var config2 = eventDefinition2.get("config", Document.class);
        assertThat(config2.getString("type")).isEqualTo(AggregationEventProcessorConfig.TYPE_NAME);
        assertThat(config2.getList("series", Document.class).get(0).getString("id")).isEqualTo("count-");
        assertThat(config2.getEmbedded(List.of("conditions", "expression", "left", "ref"), String.class)).isEqualTo("count-");
        assertThat(config2.getEmbedded(List.of("conditions", "expression", "right", "value"), Double.class)).isEqualTo(4.0);
    }
}
