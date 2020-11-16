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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.migrations.V20170110150100_FixAlertConditionsMigration.MigrationCompleted;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20170110150100_FixAlertConditionsMigrationTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public NodeId nodeId;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private MongoCollection<Document> collection;

    @Before
    public void setUp() throws Exception {
        this.clusterConfigService = spy(new ClusterConfigServiceImpl(objectMapperProvider,
                mongodb.mongoConnection(), nodeId,
                new ChainingClassLoader(getClass().getClassLoader()), new ClusterEventBus()));

        final MongoConnection mongoConnection = spy(mongodb.mongoConnection());
        final MongoDatabase mongoDatabase = spy(mongoConnection.getMongoDatabase());

        when(mongoConnection.getMongoDatabase()).thenReturn(mongoDatabase);

        this.collection = spy(mongoDatabase.getCollection("streams"));

        when(mongoDatabase.getCollection("streams")).thenReturn(collection);

        this.migration = new V20170110150100_FixAlertConditionsMigration(mongoConnection, clusterConfigService);
    }

    @Test
    public void createdAt() throws Exception {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2017-01-10T15:01:00Z"));
    }

    @Test
    @MongoDBFixtures("V20170110150100_FixAlertConditionsMigration.json")
    public void upgrade() throws Exception {
        // First check all types of the existing documents
        AlertConditionAssertions.assertThat(getAlertCondition("2fa6a415-ce0c-4a36-accc-dd9519eb06d9"))
                .hasParameter("backlog", 2)
                .hasParameter("grace", 1)
                .hasParameter("threshold_type", "MORE")
                .hasParameter("threshold", "5")
                .hasParameter("time", "1");

        AlertConditionAssertions.assertThat(getAlertCondition("393fd8b2-9b17-42d3-86b0-6e55d0f5343a"))
                .hasParameter("backlog", 0)
                .hasParameter("field", "bar")
                .hasParameter("grace", "0")
                .hasParameter("value", "baz");

        AlertConditionAssertions.assertThat(getAlertCondition("0e75404f-c0ee-40b0-8872-b1aec441ba1c"))
                .hasParameter("backlog", "0")
                .hasParameter("field", "foo")
                .hasParameter("grace", "0")
                .hasParameter("threshold_type", "HIGHER")
                .hasParameter("threshold", "0")
                .hasParameter("time", "5")
                .hasParameter("type", "MAX");

        // Run the migration that should convert all affected fields to integers
        migration.upgrade();

        // Check all types again
        AlertConditionAssertions.assertThat(getAlertCondition("2fa6a415-ce0c-4a36-accc-dd9519eb06d9"))
                .hasParameter("backlog", 2)
                .hasParameter("grace", 1)
                .hasParameter("threshold_type", "MORE")
                .hasParameter("threshold", 5)
                .hasParameter("time", 1);

        AlertConditionAssertions.assertThat(getAlertCondition("393fd8b2-9b17-42d3-86b0-6e55d0f5343a"))
                .hasParameter("backlog", 0)
                .hasParameter("field", "bar")
                .hasParameter("grace", 0)
                .hasParameter("value", "baz");

        AlertConditionAssertions.assertThat(getAlertCondition("0e75404f-c0ee-40b0-8872-b1aec441ba1c"))
                .hasParameter("backlog", 0)
                .hasParameter("field", "foo")
                .hasParameter("grace", 0)
                .hasParameter("threshold_type", "HIGHER")
                .hasParameter("threshold", 0)
                .hasParameter("time", 5)
                .hasParameter("type", "MAX");

        final MigrationCompleted migrationCompleted = clusterConfigService.get(MigrationCompleted.class);

        assertThat(migrationCompleted).isNotNull();

        assertThat(migrationCompleted.streamIds()).containsOnly("58458e442f857c314491344e", "58458e442f857c314491345e");
        assertThat(migrationCompleted.alertConditionIds()).containsOnly("2fa6a415-ce0c-4a36-accc-dd9519eb06d9", "393fd8b2-9b17-42d3-86b0-6e55d0f5343a", "0e75404f-c0ee-40b0-8872-b1aec441ba1c");
    }

    @Test
    public void upgradeWhenMigrationCompleted() throws Exception {
        clusterConfigService.write(MigrationCompleted.create(Collections.emptySet(), Collections.emptySet()));

        // Reset the spy to be able to verify that there wasn't a write
        reset(clusterConfigService);

        migration.upgrade();

        verify(collection, never()).updateOne(any(), any(Bson.class));
        verify(clusterConfigService, never()).write(any(MigrationCompleted.class));
    }

    @SuppressWarnings("unchecked")
    private Document getAlertCondition(String id) {
        final Document stream = collection.find(eq("alert_conditions.id", id)).first();
        final List<Document> alertConditions = (List<Document>) stream.get("alert_conditions");

        return alertConditions.stream()
                .filter(alertCondition -> alertCondition.get("id", String.class).equals(id))
                .findFirst().orElse(null);
    }

    private static class AlertConditionAssertions extends AbstractAssert<AlertConditionAssertions, Document> {

        public static AlertConditionAssertions assertThat(Document actual) {
            return new AlertConditionAssertions(actual);
        }

        private final String id;
        private final Document parameters;

        AlertConditionAssertions(Document actual) {
            super(actual, AlertConditionAssertions.class);

            this.id = actual.get("id", String.class);
            this.parameters = actual.get("parameters", Document.class);
        }

        AlertConditionAssertions hasParameter(String field, Object expected) {
            isNotNull();

            if (!parameters.containsKey(field)) {
                failWithMessage("Parameters do not contain field <%s>", field);
            }

            final Object actual = parameters.get(field);

            Assertions.assertThat(actual)
                    .withFailMessage("Value of field <%s> in alert condition <%s>\nExpected: <%s> (%s)\nActual:   <%s> (%s)",
                            field, id, expected, expected.getClass().getCanonicalName(), actual,
                            actual.getClass().getCanonicalName())
                    .isEqualTo(expected);

            return this;
        }
    }
}
