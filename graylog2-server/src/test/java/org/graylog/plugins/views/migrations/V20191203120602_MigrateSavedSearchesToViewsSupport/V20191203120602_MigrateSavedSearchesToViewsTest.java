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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.V20191203120602_MigrateSavedSearchesToViews.MigrationCompleted;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch.SavedSearchService;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.Search;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.SearchService;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.RandomObjectIdProvider;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.View;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.ViewService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
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
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class V20191203120602_MigrateSavedSearchesToViewsTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private SearchService searchService;
    @Mock
    private ViewService viewService;

    private Migration migration;
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    static class StaticRandomObjectIdProvider extends RandomObjectIdProvider {
        private final Date date;
        private AtomicInteger counter;

        StaticRandomObjectIdProvider(Date date) {
            super(date);
            this.date = date;
            this.counter = new AtomicInteger(0);
        }

        @Override
        public String get() {
            return new ObjectId(date, 42, (short) 23, counter.incrementAndGet()).toHexString();
        }
    }

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());

        final SavedSearchService savedSearchService = new SavedSearchService(mongodb.mongoConnection(), mapperProvider);
        final RandomObjectIdProvider randomObjectIdProvider = new StaticRandomObjectIdProvider(new Date(1575020937839L));
        final RandomUUIDProvider randomUUIDProvider = new RandomUUIDProvider(new Date(1575020937839L), 1575020937839L);

        this.migration = new V20191203120602_MigrateSavedSearchesToViews(
                clusterConfigService,
                savedSearchService,
                searchService,
                viewService,
                randomObjectIdProvider,
                randomUUIDProvider
        );
    }

    @Test
    public void runsIfNoSavedSearchesArePresent() {
        this.migration.upgrade();
    }

    @Test
    public void writesMigrationCompletedAfterSuccess() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds()).isEmpty();
    }

    @Test
    @MongoDBFixtures("sample_saved_search_relative.json")
    public void migrateSavedSearchWithRelativeTimerange() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5c7e5499f38ed7e1d8d6a613", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_relative-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_relative-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_absolute.json")
    public void migrateSavedSearchWithAbsoluteTimerange() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660b7b2d44b5813c1d7f6", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_absolute-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_absolute-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_keyword.json")
    public void migrateSavedSearchWithKeywordTimerange() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660c6b2d44b5813c1d806", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_keyword-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_keyword-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_with_stream.json")
    public void migrateSavedSearchWithStreamId() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660b7b2d44b5813c1d7f6", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_with_stream-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_with_stream-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_with_missing_fields.json")
    public void migrateSavedSearchWithMissingFields() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660b7b2d44b5813c1d7f6", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_with_missing_fields-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_with_missing_fields-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_with_empty_fields.json")
    public void migrateSavedSearchWithEmptyFields() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660b7b2d44b5813c1d7f6", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_with_missing_fields-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_with_missing_fields-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_without_message_row.json")
    public void migrateSavedSearchWithoutMessageRow() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5c7e5499f38ed7e1d8d6a613", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_without_message_row-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_without_message_row-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_relative_with_interval_field.json")
    public void migrateSavedSearchRelativeWithIntervalField() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5c7e5499f38ed7e1d8d6a613", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_relative_with_interval_field-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_relative_with_interval_field-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_absolute_with_interval_field.json")
    public void migrateSavedSearchAbsoluteWithIntervalField() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660b7b2d44b5813c1d7f6", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_absolute_with_interval_field-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_absolute_with_interval_field-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_saved_search_keyword_with_interval_field.json")
    public void migrateSavedSearchKeywordWithIntervalField() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds())
                .containsExactly(new AbstractMap.SimpleEntry<>("5de660c6b2d44b5813c1d806", "5de0e98900002a0017000002"));

        assertViewServiceCreatedViews(1, resourceFile("sample_saved_search_keyword_with_interval_field-expected_views.json"));
        assertSearchServiceCreated(1, resourceFile("sample_saved_search_keyword_with_interval_field-expected_searches.json"));
    }

    private void assertViewServiceCreatedViews(int count, String viewsCollection) throws Exception {
        final ArgumentCaptor<View> newViewsCaptor = ArgumentCaptor.forClass(View.class);
        verify(viewService, times(count)).save(newViewsCaptor.capture());
        final List<View> newViews = newViewsCaptor.getAllValues();
        assertThat(newViews).hasSize(count);

        JSONAssert.assertEquals(viewsCollection, toJSON(newViews), true);
    }

    private void assertSearchServiceCreated(int count, String searchCollection) throws Exception {
        final ArgumentCaptor<Search> newSearchesCaptor = ArgumentCaptor.forClass(Search.class);

        verify(searchService, times(count)).save(newSearchesCaptor.capture());

        final List<Search> newSearches = newSearchesCaptor.getAllValues();

        assertThat(newSearches).hasSize(count);

        JSONAssert.assertEquals(toJSON(newSearches), searchCollection, true);
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
            final Path path = Paths.get(resource.toURI());
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
