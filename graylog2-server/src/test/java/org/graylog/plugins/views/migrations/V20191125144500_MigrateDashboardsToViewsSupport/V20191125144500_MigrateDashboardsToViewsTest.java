package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
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

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class V20191125144500_MigrateDashboardsToViewsTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

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
            return new ObjectId(date, 42, (short)23, counter.incrementAndGet()).toHexString();
        }
    }

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        final DashboardsService dashboardsService = new DashboardsService(mongodb.mongoConnection(), mapperProvider);
        final SearchService searchService = new SearchService(mongodb.mongoConnection(), mapperProvider);
        final ViewService viewService = new ViewService(mongodb.mongoConnection(), mapperProvider);

        final RandomObjectIdProvider randomObjectIdProvider = new StaticRandomObjectIdProvider(new Date(1575020937839L));
        final RandomUUIDProvider randomUUIDProvider = new RandomUUIDProvider(new Date(1575020937839L), 1575020937839L);

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
        assertThat(migrationCompleted.dashboardToViewMigrationIds()).isEmpty();
        assertThat(migrationCompleted.widgetMigrationIds()).isEmpty();
    }

    @Test
    @MongoDBFixtures("sample_dashboard.json")
    public void migrateSampleDashboard() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.dashboardToViewMigrationIds())
                .containsAllEntriesOf(Collections.singletonMap("5c7fc3f9f38ed741ac154697", "5de0e98900002a0017000002"));
        assertThat(migrationCompleted.widgetMigrationIds()).containsAllEntriesOf(
                ImmutableMap.<String, Set<String>>builder()
                        .put("05b03c7b-fe23-4789-a1c8-a38a583d3ba6", ImmutableSet.of("0000016e-b690-4273-0000-016eb690426f"))
                        .put("10c1b3f9-6b34-4b34-9457-892d12b84151", ImmutableSet.of("0000016e-b690-4276-0000-016eb690426f"))
                        .put("2afb1838-24ee-489f-929f-ef7d47485021", ImmutableSet.of("0000016e-b690-4277-0000-016eb690426f"))
                        .put("40c9cf4e-0956-4dc1-9ccd-83868fa83277", ImmutableSet.of("0000016e-b690-4270-0000-016eb690426f"))
                        .put("4a192616-51d3-474e-9e18-a680f2577769", ImmutableSet.of("0000016e-b690-427c-0000-016eb690426f"))
                        .put("5020d62d-24a0-4b0c-8819-78e668cc2428", ImmutableSet.of("0000016e-b690-4272-0000-016eb690426f"))
                        .put("6f2cc355-bcbb-4b3f-be01-bfba299aa51a", ImmutableSet.of("0000016e-b690-4274-0000-016eb690426f", "0000016e-b690-4275-0000-016eb690426f"))
                        .put("76b7f7e1-76ac-486b-894b-bc31bf4808f1", ImmutableSet.of("0000016e-b690-4278-0000-016eb690426f"))
                        .put("91b37752-e3a8-4274-910f-3d66d19f1028", ImmutableSet.of("0000016e-b690-427b-0000-016eb690426f"))
                        .put("9b55d975-a5d4-4df6-8b2e-6fc7b48d52c3", ImmutableSet.of("0000016e-b690-427a-0000-016eb690426f"))
                        .put("a8eadf94-6494-4271-8c0e-3c8d08e65623", ImmutableSet.of("0000016e-b690-426f-0000-016eb690426f"))
                        .put("d9be20a1-82d7-427b-8a2d-c7ea9cd114de", ImmutableSet.of("0000016e-b690-427e-0000-016eb690426f"))
                        .put("da111daa-0d0a-47b9-98ed-8b8aa8a4f575", ImmutableSet.of("0000016e-b690-4279-0000-016eb690426f"))
                        .put("e9efdfaf-f7be-47ca-97fe-871c05a24d3c", ImmutableSet.of("0000016e-b690-4271-0000-016eb690426f"))
                        .put("f6e9d960-9cc8-4d16-b3c8-770501b2709f", ImmutableSet.of("0000016e-b690-427d-0000-016eb690426f"))
                        .build()
        );
    }

    @Test
    @MongoDBFixtures("ops_dashboards.json")
    public void migrateOpsDashboards() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.dashboardToViewMigrationIds())
                .containsAllEntriesOf(ImmutableMap.of(
                        "5ddf8ed5b2d44b2e04472992", "5de0e98900002a0017000002",
                        "5ddf8ed6b2d44b2e044729a2", "5de0e98900002a0017000004",
                        "5ddf8ed7b2d44b2e044729b1", "5de0e98900002a0017000006",
                        "5ddf8ed8b2d44b2e044729d2", "5de0e98900002a0017000008",
                        "5ddf8ed8b2d44b2e044729d8", "5de0e98900002a001700000a"));
    }

    private MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }
}
