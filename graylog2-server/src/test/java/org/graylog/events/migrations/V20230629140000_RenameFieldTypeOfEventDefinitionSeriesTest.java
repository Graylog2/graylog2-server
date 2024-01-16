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
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.migrations.Migration;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class V20230629140000_RenameFieldTypeOfEventDefinitionSeriesTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private NotificationService notificationService;

    private MongoCollection<Document> eventDefinitionsCollection;

    private Migration migration;

    @Before
    public void setUp() {
        this.migration = new V20230629140000_RenameFieldTypeOfEventDefinitionSeries(
                clusterConfigService,
                mongodb.mongoConnection(),
                notificationService
        );
        this.eventDefinitionsCollection = mongodb.mongoConnection().getMongoDatabase().getCollection("event_definitions");
    }

    @Test
    public void doesNotRunAgainIfMigrationHadCompletedBefore() {
        when(clusterConfigService.get(V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted.class))
                .thenReturn(new V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted());

        this.migration.upgrade();

        verify(clusterConfigService, never()).write(any());
    }

    @Test
    public void writesMigrationCompletedAfterSuccess() {
        this.migration.upgrade();

        final V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted).isNotNull();
    }

    @Test
    @MongoDBFixtures("V20230629140000_RenameFieldTypeOfEventDefinitionSeries/mixed_event_definitions.json")
    public void migratesEventDefinitionsProperly() {
        var expectedCollection = resourceFile("V20230629140000_RenameFieldTypeOfEventDefinitionSeries/mixed_event_definitions-after.json");
        runMigration(5, expectedCollection);
    }

    @Test
    @MongoDBFixtures("V20230629140000_RenameFieldTypeOfEventDefinitionSeries/mixed_event_definitions-after.json")
    public void doesNotChangeMigratedEventDefinitions() {
        var expectedCollection = resourceFile("V20230629140000_RenameFieldTypeOfEventDefinitionSeries/mixed_event_definitions-after.json");
        runMigration(5, expectedCollection);
    }

    private void runMigration(int expectedCount, String expectedCollection) {
        this.migration.upgrade();

        final V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted).isNotNull();
        assertThat(eventDefinitionsCollection.countDocuments()).isEqualTo(expectedCount);

        var actualCollection = collectionToJson();
        uncheckedJSONAssertEquals(expectedCollection, actualCollection);
    }

    private String collectionToJson() {
        var documents = StreamSupport.stream(this.eventDefinitionsCollection.find().spliterator(), false)
                .map(Document::toJson)
                .collect(Collectors.joining(",\n"));

        return String.format(Locale.ROOT, """
                {
                    "event_definitions": [
                        %s
                    ]
                }
                """, documents);
    }

    private V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(V20230629140000_RenameFieldTypeOfEventDefinitionSeries.MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private String resourceFile(String filename) {
        try {
            final URL resource = this.getClass().getResource(filename);
            if (resource == null) {
                Assert.fail("Unable to find resource file for test: " + filename);
            }
            final Path path = Paths.get(resource.toURI());
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void uncheckedJSONAssertEquals(String expectedCollection, String actualCollection) {
        try {
            JSONAssert.assertEquals(expectedCollection, actualCollection, true);
        } catch (JSONException e) {
            throw new RuntimeException("Error occurred while parsing test fixtures.", e);
        }
    }
}
