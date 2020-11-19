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
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class V20190304102700_MigrateMessageListStructureTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private Migration migration;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() throws Exception {
        migration = new V20190304102700_MigrateMessageListStructure(mongodb.mongoConnection(), clusterConfigService);
    }

    @Test
    public void createdAt() {
        // Test the date to detect accidental changes to it.
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2019-04-03T10:27:00Z"));
    }

    @Test
    @MongoDBFixtures("V20190304102700_MigrateMessageListStructureTest.json")
    public void testMigratingViewStructure() {
        final BasicDBObject dbQuery1 = new BasicDBObject();
        dbQuery1.put("_id", new ObjectId("58458e442f857c314491344e"));
        final MongoCollection<Document> collection = mongodb.mongoConnection()
                .getMongoDatabase()
                .getCollection("views");

        migration.upgrade();

        final FindIterable<Document> views = collection.find(dbQuery1);
        final Document view1 = views.first();

        @SuppressWarnings("unchecked")
        final List<Document> widgets1 = (List) view1.get("state", Document.class).get("a2a804b7-27cf-4cac-8015-58d9a9640d33", Document.class).get("widgets");
        assertThat(widgets1.size()).isEqualTo(2);
        assertThat(widgets1.stream().filter(widget -> widget.getString("type").equals("messages")).count()).isEqualTo(1);
        assertThat(widgets1.stream().filter(widget -> widget.getString("type").equals("messages")).allMatch((widget) -> {
            final Document config = widget.get("config", Document.class);
            @SuppressWarnings("unchecked")
            final List<String> fields = (List) config.get("fields");
            final boolean startWithTimestamp = fields.get(0).contains("timestamp");
            final boolean showMessageRow = config.getBoolean("show_message_row");
            return startWithTimestamp && showMessageRow;
        })).isTrue();

        final BasicDBObject dbQuery2 = new BasicDBObject();
        dbQuery2.put("_id", new ObjectId("58458e442f857c314491344f"));

        final FindIterable<Document> views2 = collection.find(dbQuery2);
        final Document view2 = views2.first();

        final Document states = view2.get("state", Document.class);
        assertThat(states.values().size()).isEqualTo(13);
        assertThat(states.keySet()).containsExactly(
                "7c042319-530a-41b9-9dbb-9676fb1da1a4",
                "9e5144be-a445-4289-a4cc-0f55142524bc",
                "c13b2482-60e7-4b1e-98c9-0df8d6da8230",
                "5adc9297-dfc8-4fd9-b422-cbb097715a62",
                "ade8c853-503c-407f-b125-efbe2d368973",
                "cc2bf983-b398-4295-bf01-1c10ed1a97e1",
                "64feccae-9447-40ef-a401-79a7972078a2",
                "7c7e04c6-f9f0-495c-91cc-865f60687f8c",
                "eeaa8838-616f-40c0-88c0-1059ac64f37e",
                "91c6f8c9-024c-48ec-a869-90548fad218a",
                "955a71f2-673a-4e1c-a99f-ef97b1b4ae71",
                "343ff7b6-4554-49d4-bc0b-1339fdc5dac0",
                "7a84d053-e40a-48c1-a433-97521f7ce7ef");

        states.values().forEach(state -> {
            @SuppressWarnings("unchecked")
            final List<Document> widgets2 = (List) ((Document) state).get("widgets");
            assertThat(widgets2.stream().filter(widget -> widget.getString("type").equals("messages")).count()).isGreaterThan(0);
            widgets2.stream().filter(widget -> widget.getString("type").equals("messages")).forEach((widget) -> {
                final Document config = widget.get("config", Document.class);
                @SuppressWarnings("unchecked")
                final List<String> fields = (List) config.get("fields");
                final boolean startWithTimestamp = fields.get(0).contains("timestamp");
                final boolean showMessageRow = config.getBoolean("show_message_row");
                assertThat(startWithTimestamp).isTrue();
                assertThat(showMessageRow).isTrue();
            });
        });
    }
}
