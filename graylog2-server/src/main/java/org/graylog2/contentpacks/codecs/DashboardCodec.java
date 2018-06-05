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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.TimeRangeEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.dashboards.widgets.WidgetPosition;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.graylog2.timeranges.TimeRangeFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class DashboardCodec implements EntityCodec<Dashboard> {
    private final ObjectMapper objectMapper;
    private final DashboardService dashboardService;
    private final DashboardWidgetCreator widgetCreator;
    private final TimeRangeFactory timeRangeFactory;

    @Inject
    public DashboardCodec(ObjectMapper objectMapper,
                          DashboardService dashboardService,
                          DashboardWidgetCreator widgetCreator,
                          TimeRangeFactory timeRangeFactory
    ) {
        this.objectMapper = objectMapper;
        this.dashboardService = dashboardService;
        this.widgetCreator = widgetCreator;
        this.timeRangeFactory = timeRangeFactory;
    }

    @Override
    public EntityWithConstraints encode(Dashboard dashboard) {
        final Map<String, WidgetPosition> positionsById = dashboard.getPositions().stream()
                .collect(Collectors.toMap(WidgetPosition::id, v -> v));
        final List<DashboardWidgetEntity> dashboardWidgets = dashboard.getWidgets().entrySet().stream()
                .map(widget -> encodeWidget(widget.getValue(), positionsById.get(widget.getKey())))
                .collect(Collectors.toList());
        final DashboardEntity dashboardEntity = DashboardEntity.create(
                ValueReference.of(dashboard.getTitle()),
                ValueReference.of(dashboard.getDescription()),
                dashboardWidgets);
        final JsonNode data = objectMapper.convertValue(dashboardEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(dashboard.getId()))
                .type(ModelTypes.DASHBOARD)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    private DashboardWidgetEntity encodeWidget(DashboardWidget widget, @Nullable WidgetPosition position) {
        return DashboardWidgetEntity.create(
                ValueReference.of(widget.getDescription()),
                ValueReference.of(widget.getType()),
                ValueReference.of(widget.getCacheTime()),
                TimeRangeEntity.of(widget.getTimeRange()),
                toReferenceMap(widget.getConfig()),
                encodePosition(position));
    }

    @Nullable
    private DashboardWidgetEntity.Position encodePosition(@Nullable WidgetPosition position) {
        return position == null ? null : DashboardWidgetEntity.Position.create(
                ValueReference.of(position.width()),
                ValueReference.of(position.height()),
                ValueReference.of(position.row()),
                ValueReference.of(position.col()));
    }

    @Override
    public Dashboard decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters, username);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Dashboard decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters, String username) {
        DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        final Dashboard dashboard;
        try {
            dashboard = createDashboard(
                    dashboardEntity.title().asString(parameters),
                    dashboardEntity.description().asString(parameters),
                    dashboardEntity.widgets(),
                    username,
                    parameters
            );
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create dashboard", e);
        }

        return dashboard;
    }

    private Dashboard createDashboard(
            final String title,
            final String description,
            final List<DashboardWidgetEntity> widgets,
            final String username,
            final Map<String, ValueReference> parameters)
            throws ValidationException, DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        // Create dashboard.
        final Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put(DashboardImpl.FIELD_TITLE, title);
        dashboardData.put(DashboardImpl.FIELD_DESCRIPTION, description);
        dashboardData.put(DashboardImpl.FIELD_CREATOR_USER_ID, username);
        dashboardData.put(DashboardImpl.FIELD_CREATED_AT, Tools.nowUTC());

        final org.graylog2.dashboards.Dashboard dashboard = new DashboardImpl(dashboardData);
        final String dashboardId = dashboardService.save(dashboard);

        final ImmutableList.Builder<WidgetPositionsRequest.WidgetPosition> widgetPositions = ImmutableList.builder();
        for (DashboardWidgetEntity widgetEntity : widgets) {
            final DashboardWidget widget = createDashboardWidget(
                    widgetEntity.type().asString(parameters),
                    widgetEntity.description().asString(parameters),
                    toValueMap(widgetEntity.configuration(), parameters),
                    widgetEntity.cacheTime().asInteger(parameters),
                    username);
            dashboardService.addWidget(dashboard, widget);

            widgetEntity.position().ifPresent(position -> widgetPositions.add(WidgetPositionsRequest.WidgetPosition.create(
                    widget.getId(),
                    position.col().asInteger(parameters),
                    position.row().asInteger(parameters),
                    position.height().asInteger(parameters),
                    position.width().asInteger(parameters))));
        }

        // FML: We need to reload the dashboard because not all fields (I'm looking at you, "widgets") is set in the
        // Dashboard instance used before.
        final Dashboard persistedDashboard;
        try {
            persistedDashboard = dashboardService.load(dashboardId);
            dashboardService.updateWidgetPositions(persistedDashboard, WidgetPositionsRequest.create(widgetPositions.build()));
        } catch (NotFoundException e) {
            throw new RuntimeException("Failed to load dashboard with id " + dashboardId, e);
        }

        return dashboard;
    }

    @SuppressWarnings("unchecked")
    private DashboardWidget createDashboardWidget(
            final String type,
            final String description,
            final Map<String, Object> configuration,
            final int cacheTime,
            final String username)
            throws InvalidRangeParametersException, DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {

        // Replace "stream_id" in configuration if it's set
        /*
        TODO:
        final String streamReference = (String) configuration.get("stream_id");
        if (!isNullOrEmpty(streamReference)) {
            final org.graylog2.plugin.streams.Stream stream = streamsByReferenceId.get(streamReference);
            if (null != stream) {
                configuration.put("stream_id", stream.getId());
            } else {
                LOG.warn("Couldn't find referenced stream {}", streamReference);
            }
        }
        */

        final Map<String, Object> timerangeConfig = (Map<String, Object>) configuration.get("timerange");
        final TimeRange timeRange = timeRangeFactory.create(timerangeConfig);

        final String widgetId = UUID.randomUUID().toString();
        return widgetCreator.buildDashboardWidget(type,
                widgetId, description, cacheTime,
                configuration, timeRange, username);
    }

    @Override
    public EntityExcerpt createExcerpt(Dashboard dashboard) {
        return EntityExcerpt.builder()
                .id(ModelId.of(dashboard.getId()))
                .type(ModelTypes.DASHBOARD)
                .title(dashboard.getTitle())
                .build();
    }
}
