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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.WidgetCacheTime;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.timeranges.TimeRangeFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private DashboardService dashboardService;
    private DashboardWidgetCreator widgetCreator;
    private DashboardCodec codec;

    @Before
    public void setUp() {
        final TimeRangeFactory timeRangeFactory = new TimeRangeFactory();
        final WidgetCacheTime widgetCacheTime = new WidgetCacheTime(Duration.minutes(5L), 120);
        final WidgetCacheTime.Factory widgetCacheTimeFactory = mock(WidgetCacheTime.Factory.class);
        when(widgetCacheTimeFactory.create(anyInt())).thenReturn(widgetCacheTime);

        widgetCreator = new DashboardWidgetCreator(widgetCacheTimeFactory, timeRangeFactory);

        codec = new DashboardCodec(objectMapper, dashboardService, widgetCreator, timeRangeFactory);
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

        final EntityWithConstraints entityWithConstraints = codec.encode(dashboard);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(entity.type()).isEqualTo(ModelType.of("dashboard"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entityV1.data(), DashboardEntity.class);
        assertThat(dashboardEntity.title()).isEqualTo("Dashboard Title");
        assertThat(dashboardEntity.description()).isEqualTo("Dashboard Description");
        assertThat(dashboardEntity.widgets())
                .hasSize(1)
                .first()
                .satisfies(widget -> {
                    assertThat(widget.type()).isEqualTo("some-type");
                    assertThat(widget.description()).isEqualTo("description");
                    assertThat(widget.cacheTime()).isEqualTo(120);
                    assertThat(widget.position()).hasValueSatisfying(position -> {
                        assertThat(position.width()).isEqualTo(2);
                        assertThat(position.height()).isEqualTo(2);
                        assertThat(position.row()).isEqualTo(1);
                        assertThat(position.col()).isEqualTo(1);
                    });
                    assertThat(widget.configuration()).containsEntry("some-setting", "foobar");
                    try {
                        assertThat(widget.timeRange()).isEqualTo(AbsoluteRange.create("2018-04-09T16:00:00.000Z", "2018-04-09T17:00:00.000Z"));
                    } catch (InvalidRangeParametersException e) {
                        throw new AssertionError(e);
                    }
                });
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title"
        );
        final DashboardImpl dashboard = new DashboardImpl(fields);
        final EntityExcerpt excerpt = codec.createExcerpt(dashboard);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("dashboard"));
        assertThat(excerpt.title()).isEqualTo(dashboard.getTitle());
    }
}