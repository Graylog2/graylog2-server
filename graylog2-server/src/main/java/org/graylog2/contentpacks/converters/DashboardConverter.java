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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.widgets.DashboardWidget;

import javax.inject.Inject;
import java.util.Map;

public class DashboardConverter implements EntityConverter<Dashboard> {
    private final ObjectMapper objectMapper;

    @Inject
    public DashboardConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(Dashboard dashboard) {
        final ObjectNode data = objectMapper.createObjectNode()
                .put("title", dashboard.getTitle())
                .put("description", dashboard.getDescription());

        // TODO: Create independent representation of entity?
        final ArrayNode positions = objectMapper.convertValue(dashboard.getPositions(), ArrayNode.class);
        data.set("positions", positions);

        final ArrayNode widgets = data.putArray("widgets");
        for (Map.Entry<String, DashboardWidget> entry : dashboard.getWidgets().entrySet()) {
            final DashboardWidget dashboardWidget = entry.getValue();
            final ObjectNode widget = objectMapper.convertValue(dashboardWidget.getPersistedFields(), ObjectNode.class);
            widget.remove(DashboardWidget.FIELD_CREATOR_USER_ID);

            widgets.add(widget);
        }

        return EntityV1.builder()
                .id(ModelId.of(dashboard.getId()))
                .type(ModelTypes.DASHBOARD)
                .data(data)
                .build();
    }
}
