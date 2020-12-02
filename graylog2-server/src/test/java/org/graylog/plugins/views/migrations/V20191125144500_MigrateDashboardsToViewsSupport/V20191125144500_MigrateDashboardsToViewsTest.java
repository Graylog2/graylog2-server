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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.NonImplementedWidget;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        assertViewsWritten(1,resourceFile("sample_dashboard-expected_views.json"));
        assertSearchesWritten(1, resourceFile("sample_dashboard-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_dashboard_installed_from_content_pack.json")
    public void migrateSampleDashboardInstalledFromContentPack() throws Exception {
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

        assertViewsWritten(1, resourceFile("sample_dashboard-expected_views.json"));
        assertSearchesWritten(1, resourceFile("sample_dashboard-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("dashboard_with_minimal_quickvalues_widget.json")
    public void migrateDashboardWithMinimalQuickValuesWidget() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5c7fc3f9f38ed741ac154697");
        assertThat(migrationCompleted.widgetMigrationIds()).containsAllEntriesOf(
                ImmutableMap.<String, Set<String>>builder()
                        .put("6f2cc355-bcbb-4b3f-be01-bfba299aa51a", ImmutableSet.of("0000016e-b690-426f-0000-016eb690426f", "0000016e-b690-4270-0000-016eb690426f"))
                        .build()
        );

        assertViewsWritten(1, resourceFile("dashboard_with_minimal_quickvalues_widget-expected_views.json"));
        assertSearchesWritten(1, resourceFile("dashboard_with_minimal_quickvalues_widget-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("sample_dashboard_with_unknown_widget.json")
    public void migrateSampleDashboardWithUnknownWidget() {
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
                        .put("5020d62d-24a0-4b0c-8819-78e668cc2428", ImmutableSet.of("5020d62d-24a0-4b0c-8819-78e668cc2428"))
                        .put("6f2cc355-bcbb-4b3f-be01-bfba299aa51a", ImmutableSet.of("0000016e-b690-4274-0000-016eb690426f", "0000016e-b690-4275-0000-016eb690426f"))
                        .put("76b7f7e1-76ac-486b-894b-bc31bf4808f1", ImmutableSet.of("0000016e-b690-4276-0000-016eb690426f"))
                        .put("91b37752-e3a8-4274-910f-3d66d19f1028", ImmutableSet.of("0000016e-b690-4277-0000-016eb690426f"))
                        .put("9b55d975-a5d4-4df6-8b2e-6fc7b48d52c3", ImmutableSet.of("0000016e-b690-4278-0000-016eb690426f"))
                        .put("a8eadf94-6494-4271-8c0e-3c8d08e65623", ImmutableSet.of("0000016e-b690-4279-0000-016eb690426f"))
                        .put("d9be20a1-82d7-427b-8a2d-c7ea9cd114de", ImmutableSet.of("0000016e-b690-427a-0000-016eb690426f"))
                        .put("da111daa-0d0a-47b9-98ed-8b8aa8a4f575", ImmutableSet.of("0000016e-b690-427b-0000-016eb690426f"))
                        .put("e9efdfaf-f7be-47ca-97fe-871c05a24d3c", ImmutableSet.of("0000016e-b690-427c-0000-016eb690426f"))
                        .put("f6e9d960-9cc8-4d16-b3c8-770501b2709f", ImmutableSet.of("0000016e-b690-427d-0000-016eb690426f"))
                        .build()
        );

        final ArgumentCaptor<View> viewCaptor = ArgumentCaptor.forClass(View.class);
        verify(viewService, times(1)).save(viewCaptor.capture());
        final View view = viewCaptor.getValue();
        final Optional<ViewWidget> nonImplementedWidget = view.state().get("0000016e-b690-428f-0000-016eb690426f").widgets()
                .stream()
                .filter(widget -> widget instanceof NonImplementedWidget)
                .findFirst();
        assertThat(nonImplementedWidget).isPresent();
        assertThat(nonImplementedWidget.get()).isEqualTo(NonImplementedWidget.create(
                "5020d62d-24a0-4b0c-8819-78e668cc2428",
                "TOTALLY_UNKNOWN_WIDGET",
                ImmutableMap.<String, Object>builder()
                        .put("valuetype", "total")
                        .put("renderer", "line")
                        .put("interpolation", "linear")
                        .put("timerange", ImmutableMap.<String, Object>of(
                                "type", "relative",
                                "range", 28800
                        ))
                        .put("rangeType", "relative")
                        .put("field", "nf_bytes")
                        .put("query", "")
                        .put("interval", "minute")
                        .put("relative", 28800)
                        .build()
                )
        );
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

        assertViewsWritten(5, resourceFile("ops_dashboards-expected_views.json"));
        assertSearchesWritten(5, resourceFile("ops_dashboards-expected_searches.json"));
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

    @Test
    @MongoDBFixtures("dashboard_with_no_widgets.json")
    public void migratesADashboardWithNoWidgets() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5ddf8ed5b2d44b2e04472992");
        assertThat(migrationCompleted.widgetMigrationIds()).isEmpty();

        verify(viewService, times(1)).save(any());
        verify(searchService, times(1)).save(any());
    }

    @Test
    @MongoDBFixtures("dashboard_with_no_widget_positions.json")
    public void migratesADashboardWithNoWidgetPositions() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5ddf8ed5b2d44b2e04472992");
        assertThat(migrationCompleted.widgetMigrationIds()).hasSize(16);

        verify(viewService, times(1)).save(any());
        verify(searchService, times(1)).save(any());
    }

    @Test
    @MongoDBFixtures("dashboard_with_superfluous_widget_attributes.json")
    public void migratesADashboardWithSuperfluousWidgetAttributes() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5ddf8ed5b2d44b2e04472992");
        assertThat(migrationCompleted.widgetMigrationIds()).hasSize(19);

        verify(viewService, times(1)).save(any());
        verify(searchService, times(1)).save(any());
    }

    @Test
    @MongoDBFixtures("dashboard_with_missing_quickvalues_attributes.json")
    public void migratesADashboardWithMissingQuickValuesAttributes() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5ddf8ed5b2d44b2e04472992");
        assertThat(migrationCompleted.widgetMigrationIds()).hasSize(6);

        final ArgumentCaptor<View> viewCaptor = ArgumentCaptor.forClass(View.class);
        verify(viewService, times(1)).save(viewCaptor.capture());
        verify(searchService, times(1)).save(any());
        final Set<ViewWidget> widgets = viewCaptor.getValue().state().get("0000016e-b690-427d-0000-016eb690426f").widgets();
        final Function<String, Set<ViewWidget>> findNewWidgets = (String widgetId) -> {
            final Set<String> newWidgetIds = migrationCompleted.widgetMigrationIds().get(widgetId);
            return widgets.stream().filter(widget -> newWidgetIds.contains(widget.id())).collect(Collectors.toSet());
        };

        final List<ViewWidget> widgetWithoutAttributes = new ArrayList<>(findNewWidgets.apply("4ce93e89-4771-4ce2-8b59-6dc058cbfd3b"));
        assertThat(widgetWithoutAttributes).hasSize(1);
        assertThat(((AggregationWidget)widgetWithoutAttributes.get(0)).config().visualization()).isEqualTo("table");

        final List<ViewWidget> widgetWithOnlyShowPieChartIsFalse = new ArrayList<>(findNewWidgets.apply("5c12c588-be0c-436b-b999-ee18378efd45"));
        assertThat(widgetWithOnlyShowPieChartIsFalse).hasSize(1);
        assertThat(((AggregationWidget)widgetWithOnlyShowPieChartIsFalse.get(0)).config().visualization()).isEqualTo("table");

        final List<ViewWidget> widgetWithOnlyShowDataTableIsFalse = new ArrayList<>(findNewWidgets.apply("e6a16d9a-23c0-4b7f-93b5-d790b5d64672"));
        assertThat(widgetWithOnlyShowDataTableIsFalse).hasSize(1);
        assertThat(((AggregationWidget)widgetWithOnlyShowDataTableIsFalse.get(0)).config().visualization()).isEqualTo("table");

        final List<ViewWidget> widgetWithBothAttributesPresentButFalse = new ArrayList<>(findNewWidgets.apply("568c005a-11ec-4be9-acd7-b2aa509c07e0"));
        assertThat(widgetWithBothAttributesPresentButFalse).hasSize(1);
        assertThat(((AggregationWidget)widgetWithBothAttributesPresentButFalse.get(0)).config().visualization()).isEqualTo("table");

        final List<ViewWidget> widgetWithPieChartPresentAndTrue = new ArrayList<>(findNewWidgets.apply("2e3c5e76-bbfd-4ac3-a27b-7491a5cbf59a"));
        assertThat(widgetWithPieChartPresentAndTrue).hasSize(1);
        assertThat(((AggregationWidget)widgetWithPieChartPresentAndTrue.get(0)).config().visualization()).isEqualTo("pie");

        final List<ViewWidget> widgetWithBothPieChartAndDataTablePresentAndTrue = new ArrayList<>(findNewWidgets.apply("26a0a3e1-718f-4bfe-90a2-cb441390152d"));
        assertThat(widgetWithBothPieChartAndDataTablePresentAndTrue).hasSize(2);
        assertThat(widgetWithBothPieChartAndDataTablePresentAndTrue)
                .extracting(viewWidget -> ((AggregationWidget)viewWidget).config().visualization())
                .containsExactlyInAnyOrder("table", "pie");
    }

    @Test
    @MongoDBFixtures("quickvalues_widget_with_sort_order.json")
    public void migratesAQuickValuesWidgetWithSortOrder() throws Exception {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.migratedDashboardIds()).containsExactly("5b3b76caadbe1d0001417041");
        assertThat(migrationCompleted.widgetMigrationIds()).hasSize(2);

        assertViewsWritten(1, resourceFile("quickvalues_widget_with_sort_order-expected_views.json"));
        assertSearchesWritten(1, resourceFile("quickvalues_widget_with_sort_order-expected_searches.json"));
    }

    @Test
    @MongoDBFixtures("dashboard_with_widgets_missing_query_attributes.json")
    public void migrateDashboardWithWidgetsMissingQueryAttributes() throws Exception {
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

        assertViewsWritten(1,resourceFile("dashboard_with_widgets_missing_query_attributes-expected_views.json"));
        assertSearchesWritten(1, resourceFile("dashboard_with_widgets_missing_query_attributes-expected_searches.json"));
    }

    private void assertSearchesWritten(int count, String expectedEntities) throws Exception {
        final ArgumentCaptor<Search> newSearchesCaptor = ArgumentCaptor.forClass(Search.class);
        verify(searchService, times(count)).save(newSearchesCaptor.capture());
        final List<Search> newSearches = newSearchesCaptor.getAllValues();
        assertThat(newSearches).hasSize(count);

        JSONAssert.assertEquals(expectedEntities, toJSON(newSearches), false);
    }

    private void assertViewsWritten(int count, String expectedEntities) throws Exception {
        final ArgumentCaptor<View> newViewsCaptor = ArgumentCaptor.forClass(View.class);
        verify(viewService, times(count)).save(newViewsCaptor.capture());
        final List<View> newViews = newViewsCaptor.getAllValues();
        assertThat(newViews).hasSize(count);

        JSONAssert.assertEquals(expectedEntities, toJSON(newViews), false);
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
