/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.V20191203120602_MigrateSavedSearchesToViews.MigrationCompleted;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch.SavedSearchService;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.SearchService;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.RandomObjectIdProvider;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.ViewService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Date;
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
    @MongoDBFixtures("sample_saved_searches.json")
    public void migrateSampleSavedSearches() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.savedSearchIds()).containsAllEntriesOf(
                ImmutableMap.of(
                        "5c7e5499f38ed7e1d8d6a613", "5de0e98900002a0017000002",
                        "5de660b7b2d44b5813c1d7f6", "5de0e98900002a0017000004",
                        "5de660c6b2d44b5813c1d806", "5de0e98900002a0017000006"
                )
        );
    }

    private MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }
}
