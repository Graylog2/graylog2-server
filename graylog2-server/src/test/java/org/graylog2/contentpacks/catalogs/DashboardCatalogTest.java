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
package org.graylog2.contentpacks.catalogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.graph.Graph;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.contentpacks.codecs.DashboardCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.DashboardServiceImpl;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.WidgetCacheTime;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.timeranges.TimeRangeFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardCatalogTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private DashboardCatalog catalog;

    @Before
    public void setUp() throws Exception {
        final TimeRangeFactory timeRangeFactory = new TimeRangeFactory();
        final WidgetCacheTime widgetCacheTime = new WidgetCacheTime(Duration.minutes(5L), 120);
        final WidgetCacheTime.Factory widgetCacheTimeFactory = mock(WidgetCacheTime.Factory.class);
        when(widgetCacheTimeFactory.create(anyInt())).thenReturn(widgetCacheTime);
        final DashboardWidgetCreator widgetCreator = new DashboardWidgetCreator(widgetCacheTimeFactory, timeRangeFactory);
        final DashboardService dashboardService = new DashboardServiceImpl(mongoRule.getMongoConnection(), widgetCreator);
        final DashboardCodec codec = new DashboardCodec(objectMapper, dashboardService, widgetCreator, timeRangeFactory);

        catalog = new DashboardCatalog(dashboardService, codec);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt = EntityExcerpt.builder()
                .id(ModelId.of("5a82f5974b900a7a97caa1e5"))
                .type(ModelTypes.DASHBOARD)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = catalog.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void collectEntity() {
        final Optional<EntityWithConstraints> collectedEntity = catalog.collectEntity(EntityDescriptor.create(ModelId.of("5a82f5974b900a7a97caa1e5"), ModelTypes.DASHBOARD));
        assertThat(collectedEntity)
                .isPresent()
                .map(EntityWithConstraints::entity)
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.map(EntityWithConstraints::entity).orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of("5a82f5974b900a7a97caa1e5"));
        assertThat(entity.type()).isEqualTo(ModelTypes.DASHBOARD);
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        assertThat(dashboardEntity.title()).isEqualTo("Test");
        assertThat(dashboardEntity.description()).isEqualTo("Description");
        assertThat(dashboardEntity.widgets()).hasSize(6);
    }

    @Test
    @UsingDataSet(locations = "/org/graylog2/contentpacks/dashboards.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void resolve() {
        final EntityDescriptor dashboardEntity = EntityDescriptor.create(ModelId.of("5a82f5974b900a7a97caa1e5"), ModelTypes.DASHBOARD);
        final EntityDescriptor streamEntity = EntityDescriptor.create(ModelId.of("5adf23894b900a0fdb4e517d"), ModelTypes.STREAM);

        final Graph<EntityDescriptor> graph = catalog.resolve(dashboardEntity);
        assertThat(graph.nodes())
                .containsOnly(dashboardEntity, streamEntity);
    }
}
