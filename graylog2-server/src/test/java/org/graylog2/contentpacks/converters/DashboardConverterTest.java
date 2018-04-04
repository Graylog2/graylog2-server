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
package org.graylog2.contentpacks.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.WidgetCacheTime;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.timeranges.TimeRangeFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final DashboardConverter converter = new DashboardConverter(objectMapper);

    private DashboardWidgetCreator widgetCreator;

    @Before
    public void setUp() {
        final TimeRangeFactory timeRangeFactory = new TimeRangeFactory();
        final WidgetCacheTime widgetCacheTime = new WidgetCacheTime(Duration.minutes(5L), 120);
        final WidgetCacheTime.Factory widgetCacheTimeFactory = mock(WidgetCacheTime.Factory.class);
        when(widgetCacheTimeFactory.create(anyInt())).thenReturn(widgetCacheTime);

        widgetCreator = new DashboardWidgetCreator(widgetCacheTimeFactory, timeRangeFactory);
    }

    @Test
    public void convert() throws Exception {
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

        final Entity entity = converter.convert(dashboard);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(dashboard.getId()));
        assertThat(entity.type()).isEqualTo(ModelType.of("dashboard"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final JsonNode data = entityV1.data();
        assertThat(data.path("title").asText()).isEqualTo("Dashboard Title");
        assertThat(data.path("description").asText()).isEqualTo("Dashboard Description");
        assertThat(data.path("positions").isArray()).isTrue();
        final JsonNode firstWidgetPosition = data.path("positions").get(0);
        assertThat(firstWidgetPosition.path("id").asText()).isEqualTo("widget-id");
        assertThat(firstWidgetPosition.path("width").asInt()).isEqualTo(2);
        assertThat(firstWidgetPosition.path("height").asInt()).isEqualTo(2);
        assertThat(firstWidgetPosition.path("col").asInt()).isEqualTo(1);
        assertThat(firstWidgetPosition.path("row").asInt()).isEqualTo(1);
        assertThat(data.path("widgets").isArray()).isTrue();
        final JsonNode firstWidget = data.path("widgets").get(0);
        assertThat(firstWidget.path("id").asText()).isEqualTo("widget-id");
        assertThat(firstWidget.path("type").asText()).isEqualTo("some-type");
        assertThat(firstWidget.path("description").asText()).isEqualTo("description");
        assertThat(firstWidget.path("cache_time").asInt()).isEqualTo(120);
        final JsonNode firstWidgetConfig = firstWidget.path("config");
        assertThat(firstWidgetConfig.path("some-setting").asText()).isEqualTo("foobar");
        assertThat(firstWidgetConfig.path("timerange").path("type").asText()).isEqualTo("absolute");
        assertThat(firstWidgetConfig.path("timerange").path("from").asText()).isEqualTo("2018-04-09T16:00:00.000Z");
        assertThat(firstWidgetConfig.path("timerange").path("to").asText()).isEqualTo("2018-04-09T17:00:00.000Z");
    }
}