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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.AbsoluteRangeEntity;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.DashboardServiceImpl;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.WidgetCacheTime;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.timeranges.TimeRangeFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardFacadeTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private DashboardWidgetCreator widgetCreator;
    private DashboardFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final TimeRangeFactory timeRangeFactory = new TimeRangeFactory();
        final WidgetCacheTime widgetCacheTime = new WidgetCacheTime(Duration.minutes(5L), 120);
        final WidgetCacheTime.Factory widgetCacheTimeFactory = mock(WidgetCacheTime.Factory.class);
        when(widgetCacheTimeFactory.create(anyInt())).thenReturn(widgetCacheTime);
        widgetCreator = new DashboardWidgetCreator(widgetCacheTimeFactory, timeRangeFactory);

        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final EventBus serverEventBus = new EventBus("server-event-bus");
        final DashboardService dashboardService = new DashboardServiceImpl(mongoRule.getMongoConnection(), widgetCreator, clusterEventBus, serverEventBus);

        facade = new DashboardFacade(objectMapper, dashboardService, widgetCreator, timeRangeFactory);
    }

    @Test
    public void encode() throws Exception {
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

        final EntityWithConstraints entityWithConstraints = facade.encode(dashboard);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(entity.type()).isEqualTo(ModelType.of("dashboard"));

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
                    assertThat(widget.cacheTime()).isEqualTo(ValueReference.of(120));
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
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title"
        );
        final DashboardImpl dashboard = new DashboardImpl(fields);
        final EntityExcerpt excerpt = facade.createExcerpt(dashboard);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("dashboard"));
        assertThat(excerpt.title()).isEqualTo(dashboard.getTitle());
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5a82f5974b900a7a97caa1e5"))
                .type(ModelTypes.DASHBOARD)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = facade.collectEntity(EntityDescriptor.create(ModelId.of("5a82f5974b900a7a97caa1e5"), ModelTypes.DASHBOARD));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5a82f5974b900a7a97caa1e5"));
        assertThat(entity.type()).isEqualTo(ModelTypes.DASHBOARD);
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        assertThat(dashboardEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(dashboardEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(dashboardEntity.widgets()).hasSize(6);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolve() {
        final EntityDescriptor dashboardEntity = EntityDescriptor.create(ModelId.of("5a82f5974b900a7a97caa1e5"), ModelTypes.DASHBOARD);
        final EntityDescriptor streamEntity = EntityDescriptor.create(ModelId.of("5adf23894b900a0fdb4e517d"), ModelTypes.STREAM);

        final Graph<EntityDescriptor> graph = facade.resolve(dashboardEntity);
        assertThat(graph.nodes())
                .containsOnly(dashboardEntity, streamEntity);
    }
}
