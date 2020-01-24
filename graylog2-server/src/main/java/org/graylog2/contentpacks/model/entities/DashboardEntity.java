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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.views.Titles;
import org.graylog.plugins.views.search.views.WidgetPositionDTO;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class DashboardEntity {
    @JsonProperty("title")
    @NotBlank
    public abstract ValueReference title();

    @JsonProperty("description")
    public abstract ValueReference description();

    @JsonProperty("widgets")
    @NotNull
    public abstract List<DashboardWidgetEntity> widgets();

    public static Map<String, WidgetPositionDTO>  positionMap(
            Map<String, ValueReference> parameters,
            Map<DashboardWidgetEntity, List<WidgetEntity>> widgetMap) {
        final Map<String, WidgetPositionDTO> widgetPositionMap = new HashMap<>();
        for (Map.Entry<DashboardWidgetEntity, List<WidgetEntity>> widgetEntityListEntry: widgetMap.entrySet()) {
            final DashboardWidgetEntity dashboardWidgetEntity = widgetEntityListEntry.getKey();
            Optional<DashboardWidgetEntity.Position> position = dashboardWidgetEntity.position();
            widgetEntityListEntry.getValue().forEach(widgetEntity ->
                position.ifPresent(value -> widgetPositionMap.put(widgetEntity.id(), value.convert(parameters))));
        }
        return ImmutableMap.copyOf(widgetPositionMap);
    }

    public static Titles widgetTitles(
            Map<DashboardWidgetEntity, List<WidgetEntity>> widgetMap,
            Map<String, ValueReference> parameters) {

        final Map<String, String> widgetTitleMap = new HashMap<>();

        for (Map.Entry<DashboardWidgetEntity, List<WidgetEntity>> widgetEntityListEntry: widgetMap.entrySet()) {
            widgetEntityListEntry.getValue().forEach(widgetEntity -> {
                widgetTitleMap.put(widgetEntity.id(), widgetEntityListEntry.getKey().description().asString(parameters));
            });
        }
        return Titles.withWidgetTitle(widgetTitleMap);
    }

    @JsonCreator
    public static DashboardEntity create(
            @JsonProperty("title") @NotBlank ValueReference title,
            @JsonProperty("description") ValueReference description,
            @JsonProperty("widgets") @NotNull List<DashboardWidgetEntity> widgets) {
        return new AutoValue_DashboardEntity(title, description, widgets);
    }
}
