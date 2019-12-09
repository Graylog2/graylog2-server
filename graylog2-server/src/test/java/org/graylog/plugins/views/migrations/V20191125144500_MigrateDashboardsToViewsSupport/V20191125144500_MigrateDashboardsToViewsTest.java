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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class V20191125144500_MigrateDashboardsToViewsTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    private ViewService viewService;
    private SearchService searchService;

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
            return new ObjectId(date, 42, (short)23, counter.incrementAndGet()).toHexString();
        }
    }

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        final DashboardsService dashboardsService = new DashboardsService(mongodb.mongoConnection(), mapperProvider);

        final RandomObjectIdProvider randomObjectIdProvider = new StaticRandomObjectIdProvider(new Date(1575020937839L));
        final RandomUUIDProvider randomUUIDProvider = new RandomUUIDProvider(new Date(1575020937839L), 1575020937839L);

        this.viewService = spy(new ViewService(mongodb.mongoConnection(), mapperProvider));
        this.searchService = spy(new SearchService(mongodb.mongoConnection(), mapperProvider));

        migration = new V20191125144500_MigrateDashboardsToViews(
                dashboardsService,
                searchService,
                viewService,
                clusterConfigService,
                randomObjectIdProvider,
                randomUUIDProvider
        );
    }

    @Test
    public void runsIfNoDashboardsArePresent() {
        this.migration.upgrade();
    }

    @Test
    public void writesMigrationCompletedAfterSuccess() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).isEmpty();
        assertThat(migrationCompleted.widgetMigrationIds()).isEmpty();

        verify(viewService, never()).save(any());
        verify(searchService, never()).save(any());
    }

    @Test
    @MongoDBFixtures("sample_dashboard.json")
    public void migrateSampleDashboard() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5c7fc3f9f38ed741ac154697");
        assertThat(migrationCompleted.widgetMigrationIds()).containsAllEntriesOf(
                ImmutableMap.<String, Set<String>>builder()
                        .put("05b03c7b-fe23-4789-a1c8-a38a583d3ba6", ImmutableSet.of("0000016e-b690-426f-0000-016eb690426f"))
                        .put("10c1b3f9-6b34-4b34-9457-892d12b84151", ImmutableSet.of("0000016e-b690-4270-0000-016eb690426f"))
                        .put("2afb1838-24ee-489f-929f-ef7d47485021", ImmutableSet.of("0000016e-b690-4271-0000-016eb690426f"))
                        .put("40c9cf4e-0956-4dc1-9ccd-83868fa83277", ImmutableSet.of("0000016e-b690-4272-0000-016eb690426f"))
                        .put("4a192616-51d3-474e-9e18-a680f2577769", ImmutableSet.of("0000016e-b690-4273-0000-016eb690426f"))
                        .put("5020d62d-24a0-4b0c-8819-78e668cc2428", ImmutableSet.of("0000016e-b690-4274-0000-016eb690426f"))
                        .put("6f2cc355-bcbb-4b3f-be01-bfba299aa51a", ImmutableSet.of("0000016e-b690-4275-0000-016eb690426f", "0000016e-b690-4276-0000-016eb690426f"))
                        .put("76b7f7e1-76ac-486b-894b-bc31bf4808f1", ImmutableSet.of("0000016e-b690-4277-0000-016eb690426f"))
                        .put("91b37752-e3a8-4274-910f-3d66d19f1028", ImmutableSet.of("0000016e-b690-4278-0000-016eb690426f"))
                        .put("9b55d975-a5d4-4df6-8b2e-6fc7b48d52c3", ImmutableSet.of("0000016e-b690-4279-0000-016eb690426f"))
                        .put("a8eadf94-6494-4271-8c0e-3c8d08e65623", ImmutableSet.of("0000016e-b690-427a-0000-016eb690426f"))
                        .put("d9be20a1-82d7-427b-8a2d-c7ea9cd114de", ImmutableSet.of("0000016e-b690-427b-0000-016eb690426f"))
                        .put("da111daa-0d0a-47b9-98ed-8b8aa8a4f575", ImmutableSet.of("0000016e-b690-427c-0000-016eb690426f"))
                        .put("e9efdfaf-f7be-47ca-97fe-871c05a24d3c", ImmutableSet.of("0000016e-b690-427d-0000-016eb690426f"))
                        .put("f6e9d960-9cc8-4d16-b3c8-770501b2709f", ImmutableSet.of("0000016e-b690-427e-0000-016eb690426f"))
                        .build()
        );

        final ArgumentCaptor<View> newViewsCaptor = ArgumentCaptor.forClass(View.class);
        final ArgumentCaptor<Search> newSearchesCaptor = ArgumentCaptor.forClass(Search.class);

        verify(viewService, times(1)).save(newViewsCaptor.capture());
        verify(searchService, times(1)).save(newSearchesCaptor.capture());

        final List<View> newViews = newViewsCaptor.getAllValues();
        final List<Search> newSearches = newSearchesCaptor.getAllValues();

        assertThat(newViews).hasSize(1);
        assertThat(newSearches).hasSize(1);

        JSONAssert.assertEquals(toJSON(newViews), resourceFile("sample_dashboard-expected_views.json"), false);
        JSONAssert.assertEquals(toJSON(newSearches), resourceFile("sample_dashboard-expected_searches.json"), false);
    }

    @Test
    @MongoDBFixtures("ops_dashboards.json")
    public void migrateOpsDashboards() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds())
                .containsExactlyInAnyOrder(
                        "5ddf8ed5b2d44b2e04472992",
                        "5ddf8ed6b2d44b2e044729a2",
                        "5ddf8ed7b2d44b2e044729b1",
                        "5ddf8ed8b2d44b2e044729d2",
                        "5ddf8ed8b2d44b2e044729d8"
                );

        final ArgumentCaptor<View> newViewsCaptor = ArgumentCaptor.forClass(View.class);
        final ArgumentCaptor<Search> newSearchesCaptor = ArgumentCaptor.forClass(Search.class);

        verify(viewService, times(5)).save(newViewsCaptor.capture());
        verify(searchService, times(5)).save(newSearchesCaptor.capture());

        final List<View> newViews = newViewsCaptor.getAllValues();
        final List<Search> newSearches = newSearchesCaptor.getAllValues();

        assertThat(newViews).hasSize(5);
        assertThat(newSearches).hasSize(5);

        JSONAssert.assertEquals(toJSON(newViews), resourceFile("ops_dashboards-expected_views.json"), false);
        JSONAssert.assertEquals(toJSON(newSearches), resourceFile("ops_dashboards-expected_searches.json"), false);
    }

    @Test
    @MongoDBFixtures("ops_dashboards.json")
    public void exceptionWhenSavingViewCausesRollback() throws Exception {
        doCallRealMethod()
                .doCallRealMethod()
                .doThrow(new RuntimeException("Something bad happened while saving view"))
                .when(viewService)
                .save(any());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(migration::upgrade)
                .withMessage("Something bad happened while saving view");

        verify(viewService, times(2)).remove(any());
        verify(searchService, times(3)).remove(any());

        assertThat(viewService.count()).isZero();
        assertThat(searchService.count()).isZero();
    }

    @Test
    @MongoDBFixtures("ops_dashboards.json")
    public void exceptionWhenSavingSearchCausesRollback() throws Exception {
        doCallRealMethod()
                .doCallRealMethod()
                .doThrow(new RuntimeException("Something bad happened while saving view"))
                .when(searchService)
                .save(any());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(migration::upgrade)
                .withMessage("Something bad happened while saving view");

        verify(viewService, times(2)).remove(any());
        verify(searchService, times(2)).remove(any());

        assertThat(viewService.count()).isZero();
        assertThat(searchService.count()).isZero();
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
