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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards.V20200204122000_MigrateUntypedViewsToDashboards;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards.V20200204122000_MigrateUntypedViewsToDashboards.MigrationCompleted;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class V20200204122000_MigrateUntypedViewsToDashboardsTest {
    private static final String COLLECTION_VIEWS = "views";
    private static final String COLLECTION_SEARCHES = "searches";

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private Migration migration;
    private MongoCollection<Document> viewsCollection;
    private MongoCollection<Document> searchesCollection;

    @Before
    public void setUp() throws Exception {
        this.viewsCollection = spy(mongodb.mongoConnection().getMongoDatabase().getCollection(COLLECTION_VIEWS));
        this.searchesCollection = spy(mongodb.mongoConnection().getMongoDatabase().getCollection(COLLECTION_SEARCHES));
        final MongoConnection mongoConnection = mock(MongoConnection.class);
        when(mongoConnection.getMongoDatabase()).thenReturn(mock(MongoDatabase.class));
        when(mongoConnection.getMongoDatabase().getCollection(COLLECTION_VIEWS)).thenReturn(viewsCollection);
        when(mongoConnection.getMongoDatabase().getCollection(COLLECTION_SEARCHES)).thenReturn(searchesCollection);
        this.migration = new V20200204122000_MigrateUntypedViewsToDashboards(mongoConnection, clusterConfigService);
    }

    @Test
    public void runsIfNoViewsArePresent() {
        this.migration.upgrade();
    }

    @Test
    public void writesMigrationCompletedAfterSuccess() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.viewIds()).isEmpty();
    }

    @Test
    public void doesNotRunAgainIfMigrationHadCompletedBefore() {
        when(clusterConfigService.get(MigrationCompleted.class)).thenReturn(MigrationCompleted.create(Collections.emptyList()));

        this.migration.upgrade();

        verify(clusterConfigService, never()).write(any());
        verify(this.viewsCollection, never()).find(any(Bson.class));
        verify(this.searchesCollection, never()).find(any(Bson.class));
        verify(this.viewsCollection, never()).updateOne(any(BasicDBObject.class), any(Document.class));
        verify(this.searchesCollection, never()).updateOne(any(BasicDBObject.class), any(Document.class));
    }

    @Test
    @MongoDBFixtures("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_widgets_with_filter.json")
    public void migratesWidgetFiltersToWidgetQueries() throws Exception {
        this.migration.upgrade();

        assertViewsMigrated("5c8a613a844d02001a1fd2f4");

        assertSavedViews(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_widgets_with_filter-views_after.json"));
        assertSavedSearches(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_widgets_with_filter-searches_after.json"));
    }

    @Test
    @MongoDBFixtures("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_widgets_with_filter_and_query.json")
    public void migratesWidgetFiltersToWidgetQueriesAndConcatenatesToExistingQuery() throws Exception {
        this.migration.upgrade();

        assertViewsMigrated("5c8a613a844d02001a1fd2f4");

        assertSavedViews(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_widgets_with_filter_and_query-views_after.json"));
        assertSavedSearches(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_widgets_with_filter_and_query-searches_after.json"));
    }

    @Test
    @MongoDBFixtures("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_no_widgets.json")
    public void migratesUntypedViewWithNoWidgets() throws Exception {
        this.migration.upgrade();

        assertViewsMigrated("5c8a613a844d02001a1fd2f4");

        assertSavedViews(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_no_widgets-views_after.json"));
        assertSavedSearches(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/untyped_view_with_no_widgets-searches_after.json"));
    }

    @Test
    @MongoDBFixtures("V20200204122000_MigrateUntypedViewsToDashboardsTest/typed_views.json")
    public void doesNotChangeTypedViews() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.viewIds()).isEmpty();

        verify(this.viewsCollection, times(1)).find(any(Bson.class));
        verify(this.viewsCollection, never()).updateOne(any(BasicDBObject.class), any(Document.class));
        verify(this.searchesCollection, never()).updateOne(any(BasicDBObject.class), any(Document.class));
    }

    @Test
    @MongoDBFixtures("V20200204122000_MigrateUntypedViewsToDashboardsTest/mixed_typed_and_untyped_views.json")
    public void migratesOnlyUntypedViewsIfMixedOnesArePresent() throws Exception {
        this.migration.upgrade();

        assertViewsMigrated("5c8a613a844d02001a1fd2f4");

        assertSavedViews(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/mixed_typed_and_untyped_views-views_after.json"));
        assertSavedSearches(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/mixed_typed_and_untyped_views-searches_after.json"));
    }

    @Test
    @MongoDBFixtures("V20200204122000_MigrateUntypedViewsToDashboardsTest/query_with_query_string.json")
    public void migratesQueryStringFromQueryIntoWidgetsAndSearchTypes() throws Exception {
        this.migration.upgrade();

        assertViewsMigrated("5c8a613a844d02001a1fd2f4");

        assertSavedViews(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/query_with_query_string-views_after.json"));
        assertSavedSearches(1, resourceFile("V20200204122000_MigrateUntypedViewsToDashboardsTest/query_with_query_string-searches_after.json"));
    }

    private void assertViewsMigrated(String... viewId) {
        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.viewIds()).containsExactlyInAnyOrder(viewId);
    }

    private void assertSavedViews(int count, String viewsCollection) {
        assertEntityCollection(count, viewsCollection, this.viewsCollection);
    }

    private void assertSavedSearches(int count, String searchesCollection) {
        assertEntityCollection(count, searchesCollection, this.searchesCollection);
    }

    private void assertEntityCollection(int count, String expectedCollection, MongoCollection<Document> actualCollection) {
        final ArgumentCaptor<Document> newEntitiesCaptor = ArgumentCaptor.forClass(Document.class);
        verify(actualCollection, times(count)).updateOne(any(), newEntitiesCaptor.capture());
        final List<Document> newEntities = newEntitiesCaptor.getAllValues();
        assertThat(newEntities).hasSize(count);

        uncheckedJSONAssertEquals(expectedCollection, newEntities);
    }

    private void uncheckedJSONAssertEquals(String expectedCollection, List<Document> actualEntities) {
        try {
            JSONAssert.assertEquals(expectedCollection, toJSON(actualEntities), true);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error occurred while parsing entities generated from test. ", e);
        } catch (JSONException e) {
            throw new RuntimeException("Error occurred while parsing test fixtures.", e);
        }
    }

    private MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private String toJSON(Object object) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
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
}
