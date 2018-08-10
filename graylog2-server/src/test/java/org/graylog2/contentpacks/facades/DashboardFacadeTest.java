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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.graph.Graph;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.AbsoluteRangeEntity;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.RelativeRangeEntity;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.DashboardServiceImpl;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.WidgetCacheTime;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.timeranges.TimeRangeFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private DashboardService dashboardService;
    private DashboardWidgetCreator widgetCreator;
    private DashboardFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final TimeRangeFactory timeRangeFactory = mock(TimeRangeFactory.class);
        when(timeRangeFactory.create(anyMap())).thenReturn(RelativeRange.create(300));
        final WidgetCacheTime widgetCacheTime = new WidgetCacheTime(Duration.minutes(5L), 120);
        final WidgetCacheTime.Factory widgetCacheTimeFactory = mock(WidgetCacheTime.Factory.class);
        when(widgetCacheTimeFactory.create(anyInt())).thenReturn(widgetCacheTime);
        widgetCreator = new DashboardWidgetCreator(widgetCacheTimeFactory, timeRangeFactory);

        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final EventBus serverEventBus = new EventBus("server-event-bus");
        dashboardService = new DashboardServiceImpl(mongoRule.getMongoConnection(), widgetCreator, clusterEventBus, serverEventBus);
        facade = new DashboardFacade(objectMapper, dashboardService, widgetCreator, timeRangeFactory);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportNativeEntity() throws Exception {
        final DBObject widgetPositions = new BasicDBObjectBuilder()
                .push("widget-id")
                .append("width", 2)
                .append("height", 2)
                .append("col", 1)
                .append("row", 1)
                .get();
        final DashboardWidget dashboardWidget = widgetCreator.buildDashboardWidget(
                "some-type",
                "widget-id",
                "description",
                120,
                ImmutableMap.of("some-setting", "foobar"),
                AbsoluteRange.create(DateTime.parse("2018-04-09T16:00:00.000Z"), DateTime.parse("2018-04-09T17:00:00.000Z")),
                "admin"
        );
        final Map<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title",
                DashboardImpl.FIELD_DESCRIPTION, "Dashboard Description",
                DashboardImpl.EMBEDDED_POSITIONS, widgetPositions
        );
        final DashboardImpl dashboard = new DashboardImpl(fields);
        dashboard.addWidget(dashboardWidget);

        final EntityWithConstraints entityWithConstraints = facade.exportNativeEntity(dashboard);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(entity.type()).isEqualTo(ModelTypes.DASHBOARD_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entityV1.data(), DashboardEntity.class);
        assertThat(dashboardEntity.title()).isEqualTo(ValueReference.of("Dashboard Title"));
        assertThat(dashboardEntity.description()).isEqualTo(ValueReference.of("Dashboard Description"));
        assertThat(dashboardEntity.widgets())
                .hasSize(1)
                .first()
                .satisfies(widget -> {
                    assertThat(widget.type()).isEqualTo(ValueReference.of("some-type"));
                    assertThat(widget.description()).isEqualTo(ValueReference.of("description"));
                    assertThat(widget.position()).hasValueSatisfying(position -> {
                        assertThat(position.width()).isEqualTo(ValueReference.of(2));
                        assertThat(position.height()).isEqualTo(ValueReference.of(2));
                        assertThat(position.row()).isEqualTo(ValueReference.of(1));
                        assertThat(position.col()).isEqualTo(ValueReference.of(1));
                    });
                    assertThat(widget.configuration()).containsEntry("some-setting", ValueReference.of("foobar"));
                    final AbsoluteRangeEntity expectedTimeRange = AbsoluteRangeEntity.of(
                            AbsoluteRange.create(
                                    DateTime.parse("2018-04-09T16:00:00.000Z"), DateTime.parse("2018-04-09T17:00:00.000Z"))
                    );
                    assertThat(widget.timeRange()).isEqualTo(expectedTimeRange);
                });
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5a82f5974b900a7a97caa1e5", ModelTypes.DASHBOARD_V1);
        final Optional<EntityWithConstraints> entityWithConstraints = facade.exportEntity(descriptor);
        final Entity entity = entityWithConstraints.orElseThrow(AssertionError::new).entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("5a82f5974b900a7a97caa1e5"));
        assertThat(entity.type()).isEqualTo(ModelTypes.DASHBOARD_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entityV1.data(), DashboardEntity.class);
        assertThat(dashboardEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(dashboardEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(dashboardEntity.widgets())
                .hasSize(6)
                .anySatisfy(widget -> {
                    assertThat(widget.type()).isEqualTo(ValueReference.of("SEARCH_RESULT_CHART"));
                    assertThat(widget.description()).isEqualTo(ValueReference.of("Histogram"));
                    assertThat(widget.configuration()).containsEntry("interval", ValueReference.of("minute"));
                });
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title"
        );
        final DashboardImpl dashboard = new DashboardImpl(fields);
        final EntityExcerpt excerpt = facade.createExcerpt(dashboard);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.DASHBOARD_V1);
        assertThat(excerpt.title()).isEqualTo(dashboard.getTitle());
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5a82f5974b900a7a97caa1e5"))
                .type(ModelTypes.DASHBOARD_V1)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.exportEntity(EntityDescriptor.create("5a82f5974b900a7a97caa1e5", ModelTypes.DASHBOARD_V1));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5a82f5974b900a7a97caa1e5"));
        assertThat(entity.type()).isEqualTo(ModelTypes.DASHBOARD_V1);
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        assertThat(dashboardEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(dashboardEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(dashboardEntity.widgets()).hasSize(6);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolve() {
        final EntityDescriptor dashboardEntity = EntityDescriptor.create("5a82f5974b900a7a97caa1e5", ModelTypes.DASHBOARD_V1);
        final EntityDescriptor streamEntity = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);

        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(dashboardEntity);
        assertThat(graph.nodes())
                .containsOnly(dashboardEntity, streamEntity);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() throws NotFoundException {
        final Dashboard dashboard = dashboardService.load("5a82f5974b900a7a97caa1e5");

        assertThat(dashboardService.count()).isEqualTo(1);
        facade.delete(dashboard);
        assertThat(dashboardService.count()).isEqualTo(0);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5a82f5974b900a7a97caa1e5", ModelTypes.DASHBOARD_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        final EntityDescriptor expectedStream = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);

        assertThat(graph.nodes()).containsOnly(descriptor, expectedStream);
    }

    @Test
    public void resolveEntity() throws InvalidRangeParametersException {
        final DashboardWidgetEntity dashboardWidgetEntity = DashboardWidgetEntity.create(
                ValueReference.of("12345"),
                ValueReference.of("Description"),
                ValueReference.of("type"),
                ValueReference.of(120),
                RelativeRangeEntity.of(RelativeRange.create(300)),
                new ReferenceMap(Collections.singletonMap("stream_id", ValueReference.of("stream-id"))),
                null);
        final Entity dashboardEntity = EntityV1.builder()
                .id(ModelId.of("id"))
                .type(ModelTypes.DASHBOARD_V1)
                .data(objectMapper.convertValue(DashboardEntity.create(
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        Collections.singletonList(dashboardWidgetEntity)), JsonNode.class))
                .build();
        final EntityDescriptor streamDescriptor = EntityDescriptor.create("stream-id", ModelTypes.STREAM_V1);
        final Entity streamEntity = EntityV1.builder()
                .id(ModelId.of("stream-id"))
                .type(ModelTypes.STREAM_V1)
                .data(objectMapper.convertValue(StreamEntity.create(
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        ValueReference.of(false),
                        ValueReference.of("AND"),
                        Collections.emptyList(),
                        Collections.emptySet(),
                        ValueReference.of(false),
                        ValueReference.of(false)), JsonNode.class))
                .build();
        final Map<EntityDescriptor, Entity> entityDescriptorVMap = Collections.singletonMap(streamDescriptor, streamEntity);
        final Graph<Entity> graph = facade.resolveForInstallation(dashboardEntity, Collections.emptyMap(), entityDescriptorVMap);

        assertThat(graph.nodes()).containsOnly(dashboardEntity, streamEntity);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("5a82f5974b900a7a97caa1e5"))
                .type(ModelTypes.DASHBOARD_V1)
                .data(NullNode.getInstance())
                .build();
        assertThat(facade.findExisting(entity, Collections.emptyMap())).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void createNativeEntity() throws InvalidRangeParametersException {
        final DashboardWidgetEntity dashboardWidgetEntity = DashboardWidgetEntity.create(
                ValueReference.of("12345"),
                ValueReference.of("Description"),
                ValueReference.of("type"),
                ValueReference.of(120),
                RelativeRangeEntity.of(RelativeRange.create(300)),
                ReferenceMapUtils.toReferenceMap(Collections.singletonMap("timerange", ImmutableMap.of(
                        "type", ValueReference.of("relative"),
                        "range", ValueReference.of(300)))),
                null);
        final Entity dashboardEntity = EntityV1.builder()
                .id(ModelId.of("id"))
                .type(ModelTypes.DASHBOARD_V1)
                .data(objectMapper.convertValue(DashboardEntity.create(
                        ValueReference.of("Title"),
                        ValueReference.of("Description"),
                        Collections.singletonList(dashboardWidgetEntity)), JsonNode.class))
                .build();

        assertThat(dashboardService.count()).isEqualTo(0);
        final NativeEntity<Dashboard> nativeEntity = facade.createNativeEntity(dashboardEntity, Collections.emptyMap(), Collections.emptyMap(), "admin");
        assertThat(dashboardService.count()).isEqualTo(1);

        final List<Dashboard> allDashboards = dashboardService.all();
        assertThat(allDashboards).hasSize(1);

        final Dashboard savedDashboard = allDashboards.get(0);
        final Dashboard dashboard = nativeEntity.entity();
        assertThat(dashboard).isEqualTo(savedDashboard);

        assertThat(dashboard.getWidgets().size()).isEqualTo(1);
        assertThat(dashboard.getWidgets()).containsKeys("12345");

        final NativeEntityDescriptor expectedDescriptor = NativeEntityDescriptor.create(dashboardEntity.id(), savedDashboard.getId(), ModelTypes.DASHBOARD_V1);
        assertThat(nativeEntity.descriptor()).isEqualTo(expectedDescriptor);
    }
}
