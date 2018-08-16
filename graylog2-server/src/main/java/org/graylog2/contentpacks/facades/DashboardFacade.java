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
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
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
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.graylog2.timeranges.TimeRangeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class DashboardFacade implements EntityFacade<Dashboard> {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.DASHBOARD_V1;

    private final ObjectMapper objectMapper;
    private final DashboardService dashboardService;
    private final DashboardWidgetCreator widgetCreator;
    private final TimeRangeFactory timeRangeFactory;

    @Inject
    public DashboardFacade(ObjectMapper objectMapper,
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
    public EntityWithConstraints exportNativeEntity(Dashboard dashboard) {
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
                .type(ModelTypes.DASHBOARD_V1)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    private DashboardWidgetEntity encodeWidget(DashboardWidget widget, @Nullable WidgetPosition position) {
        return DashboardWidgetEntity.create(
                ValueReference.of(widget.getId()),
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
    public NativeEntity<Dashboard> createNativeEntity(Entity entity,
                                                      Map<String, ValueReference> parameters,
                                                      Map<EntityDescriptor, Object> nativeEntities,
                                                      String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities, username);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<Dashboard> decode(EntityV1 entity,
                                           Map<String, ValueReference> parameters,
                                           Map<EntityDescriptor, Object> nativeEntities,
                                           String username) {
        DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        final Dashboard dashboard;
        try {
            dashboard = createDashboard(
                    dashboardEntity.title().asString(parameters),
                    dashboardEntity.description().asString(parameters),
                    dashboardEntity.widgets(),
                    username,
                    parameters,
                    nativeEntities
            );
        } catch (Exception e) {
            throw new ContentPackException("Couldn't create dashboard", e);
        }

        return NativeEntity.create(entity.id(), dashboard.getId(), TYPE_V1, dashboard);
    }

    private Dashboard createDashboard(
            final String title,
            final String description,
            final List<DashboardWidgetEntity> widgets,
            final String username,
            final Map<String, ValueReference> parameters,
            Map<EntityDescriptor, Object> nativeEntities)
            throws ValidationException, DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        // Create dashboard.
        final Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put(DashboardImpl.FIELD_TITLE, title);
        dashboardData.put(DashboardImpl.FIELD_DESCRIPTION, description);
        dashboardData.put(DashboardImpl.FIELD_CREATOR_USER_ID, username);
        dashboardData.put(DashboardImpl.FIELD_CREATED_AT, Tools.nowUTC());

        final Dashboard dashboard = new DashboardImpl(dashboardData);
        final String dashboardId = dashboardService.save(dashboard);

        final ImmutableList.Builder<WidgetPositionsRequest.WidgetPosition> widgetPositions = ImmutableList.builder();
        for (DashboardWidgetEntity widgetEntity : widgets) {
            final DashboardWidget widget = createDashboardWidget(
                    widgetEntity.id().asString(parameters),
                    widgetEntity.type().asString(parameters),
                    widgetEntity.description().asString(parameters),
                    toValueMap(widgetEntity.configuration(), parameters),
                    widgetEntity.cacheTime().asInteger(parameters),
                    username,
                    nativeEntities);
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

        return persistedDashboard;
    }

    @SuppressWarnings("unchecked")
    private DashboardWidget createDashboardWidget(
            final String id,
            final String type,
            final String description,
            final Map<String, Object> configuration,
            final int cacheTime,
            final String username,
            Map<EntityDescriptor, Object> nativeEntities)
            throws InvalidRangeParametersException, DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {

        final Map<String, Object> widgetConfig = new HashMap<>(configuration);

        // Replace "stream_id" in configuration if it's set
        final String streamReference = (String) widgetConfig.get("stream_id");
        if (!isNullOrEmpty(streamReference)) {
            final EntityDescriptor streamDescriptor = EntityDescriptor.create(streamReference, ModelTypes.STREAM_V1);
            final Object stream = nativeEntities.get(streamDescriptor);

            if (stream == null) {
                final String msg = "Missing stream for dashboard widget \"" + description + "\": " + streamDescriptor;
                throw new ContentPackException(msg);
            } else if (stream instanceof Stream) {
                widgetConfig.put("stream_id", ((Stream) stream).getId());
            } else {
                final String msg = "Invalid entity type for referenced stream " + streamDescriptor
                        + " for dashboard widget \"" + description + "\": " + stream.getClass();
                throw new ContentPackException(msg);
            }
        }

        final Map<String, Object> timerangeConfig = (Map<String, Object>) widgetConfig.get("timerange");
        final TimeRange timeRange = timeRangeFactory.create(timerangeConfig);

        return widgetCreator.buildDashboardWidget(type,
                id, description, cacheTime,
                widgetConfig, timeRange, username);
    }

    @Override
    public void delete(Dashboard nativeEntity) {
        dashboardService.destroy(nativeEntity);
    }

    @Override
    public EntityExcerpt createExcerpt(Dashboard dashboard) {
        return EntityExcerpt.builder()
                .id(ModelId.of(dashboard.getId()))
                .type(ModelTypes.DASHBOARD_V1)
                .title(dashboard.getTitle())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return dashboardService.all().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final Dashboard dashboard = dashboardService.load(modelId.id());
            return Optional.of(exportNativeEntity(dashboard));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find dashboard {}", entityDescriptor, e);
            return Optional.empty();
        }
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        try {
            final Dashboard dashboard = dashboardService.load(modelId.id());
            for (DashboardWidget widget : dashboard.getWidgets().values()) {
                final String streamId = (String) widget.getConfig().get("stream_id");
                if (!isNullOrEmpty(streamId)) {
                    LOG.debug("Adding stream <{}> as dependency of widget <{}> on dashboard <{}>",
                            streamId, widget.getId(), dashboard.getId());
                    final EntityDescriptor stream = EntityDescriptor.create(streamId, ModelTypes.STREAM_V1);
                    mutableGraph.putEdge(entityDescriptor, stream);
                }
            }
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find dashboard {}", entityDescriptor, e);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveEntityV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveEntityV1(EntityV1 entity,
                                          Map<String, ValueReference> parameters,
                                          Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        dashboardEntity.widgets().stream()
                .map(widget -> widget.configuration().get("stream_id"))
                .filter(ref -> ref instanceof ValueReference)
                .map(ref -> (ValueReference) ref)
                .map(valueReference -> valueReference.asString(parameters))
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.STREAM_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(stream -> mutableGraph.putEdge(entity, stream));

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
