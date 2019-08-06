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
package org.graylog.events.contentpack.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.events.contentpack.entities.AggregationEventProcessorConfigEntity;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.contentpack.entities.EventNotificationHandlerConfigEntity;
import org.graylog.events.contentpack.entities.EventProcessorConfigEntity;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EventDefinitionFacade implements EntityFacade<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionFacade.class);

    private final ObjectMapper objectMapper;
    private final EventDefinitionHandler eventDefinitionHandler;
    private final DBEventDefinitionService eventDefinitionService;
    private final DBNotificationService notificationService;

    private StreamService streamService;

    @Inject
    public EventDefinitionFacade(ObjectMapper objectMapper,
                                 EventDefinitionHandler eventDefinitionHandler,
                                 DBEventDefinitionService eventDefinitionService,
                                 StreamService streamService,
                                 DBNotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.eventDefinitionHandler = eventDefinitionHandler;
        this.eventDefinitionService = eventDefinitionService;
        this.notificationService = notificationService;
        this.streamService = streamService;
    }

    @VisibleForTesting
    private Entity exportNativeEntity(EventDefinitionDto eventDefinition, EntityDescriptorIds entityDescriptorIds) {
        final EventDefinitionEntity entity = eventDefinition.toContentPackEntity(entityDescriptorIds);

        final JsonNode data = objectMapper.convertValue(entity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(eventDefinition.id(), ModelTypes.EVENT_DEFINITION_V1)))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .data(data)
                .build();
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Optional<EventDefinitionDto> eventDefinition = eventDefinitionService.get(modelId.id());
        if (!eventDefinition.isPresent()) {
            LOG.debug("Couldn't find event definition {}", entityDescriptor);
            return Optional.empty();
        }
        return Optional.of(exportNativeEntity(eventDefinition.get(), entityDescriptorIds));
    }

    @Override
    public NativeEntity<EventDefinitionDto> createNativeEntity(Entity entity,
                                                            Map<String, ValueReference> parameters,
                                                            Map<EntityDescriptor, Object> nativeEntities,
                                                            String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<EventDefinitionDto> decode(EntityV1 entity,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Object> natvieEntities) {
        final EventDefinitionEntity eventDefinitionEntity = objectMapper.convertValue(entity.data(),
                EventDefinitionEntity.class);
        final EventDefinitionDto eventDefinition = eventDefinitionEntity.toNativeEntity(parameters, natvieEntities);
        final EventDefinitionDto savedDto = eventDefinitionHandler.create(eventDefinition);
        return NativeEntity.create(entity.id(), savedDto.id(), ModelTypes.EVENT_DEFINITION_V1, savedDto.title(), savedDto);
    }

    @Override
    public Optional<NativeEntity<EventDefinitionDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        final Optional<EventDefinitionDto> eventDefinition = eventDefinitionService.get(nativeEntityDescriptor.id().id());

        return eventDefinition.map(eventDefinitionDto ->
                NativeEntity.create(nativeEntityDescriptor, eventDefinitionDto));
    }

    @Override
    public void delete(EventDefinitionDto nativeEntity) {
        eventDefinitionHandler.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(EventDefinitionDto nativeEntity) {
        return EntityExcerpt.builder()
                .id(ModelId.of(nativeEntity.id()))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .title(nativeEntity.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return eventDefinitionService.streamAll()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<EventDefinitionDto> eventDefinition = eventDefinitionService.get(modelId.id());
        if (!eventDefinition.isPresent()) {
            LOG.debug("Couldn't find event definition {}", entityDescriptor);
        }

        //noinspection OptionalGetWithoutIsPresent
        resolveNotifications(entityDescriptor, eventDefinition.get(), mutableGraph);
        resolveStreams(entityDescriptor, eventDefinition.get(), mutableGraph);

        return ImmutableGraph.copyOf(mutableGraph);
    }

    private void resolveNotifications(EntityDescriptor entityDescriptor,
                                      EventDefinitionDto eventDefinition,
                                      MutableGraph<EntityDescriptor> mutableGraph) {
        eventDefinition.notifications().stream().map(EventNotificationHandler.Config::notificationId)
            .forEach(id -> {
                notificationService.get(id).ifPresent(notification -> {
                    final EntityDescriptor depNotification = EntityDescriptor.builder()
                        .id(ModelId.of(notification.id()))
                        .type(ModelTypes.NOTIFICATION_V1)
                        .build();
                    mutableGraph.putEdge(entityDescriptor, depNotification);
                });
            });
    }

    private void resolveStreams(EntityDescriptor entityDescriptor,
                                EventDefinitionDto eventDefinition,
                                MutableGraph<EntityDescriptor> mutableGraph) {
        if(eventDefinition.config() instanceof AggregationEventProcessorConfig) {
            AggregationEventProcessorConfig config = (AggregationEventProcessorConfig) eventDefinition.config();
            config.streams().stream().map(streamId -> {
                try {
                    return Optional.of(streamService.load(streamId));
                } catch (NotFoundException e) {
                    LOG.debug("Couldn't find stream for {}.", entityDescriptor, e);
                    return Optional.empty();
                }
            }).filter(Optional::isPresent).map(Optional::get)
                .forEach(streamObj -> {
                    final Stream stream = (Stream) streamObj;
                    final EntityDescriptor depStream = EntityDescriptor.builder()
                        .id(ModelId.of(stream.getId()))
                        .type(ModelTypes.STREAM_V1)
                        .build();
                    mutableGraph.putEdge(entityDescriptor, depStream);
                });
        }
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Entity> entities) {
        if(entity instanceof EntityV1) {
            return resolveForInstallationV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveForInstallationV1(EntityV1 entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> graph = GraphBuilder.directed().build();
        graph.addNode(entity);

        final EventDefinitionEntity eventDefinition = objectMapper.convertValue(entity.data(), EventDefinitionEntity.class);
        resolveNotificationsForInstallation(entity, eventDefinition, parameters, entities, graph);
        resolveStreamsForInstallation(entity, eventDefinition, parameters, entities, graph);

        return ImmutableGraph.copyOf(graph);
    }

    private void resolveNotificationsForInstallation(EntityV1 entity,
                                      EventDefinitionEntity eventDefinitionEntity,
                                      Map<String, ValueReference> parameters,
                                      Map<EntityDescriptor, Entity> entities,
                                      MutableGraph<Entity> graph) {
        eventDefinitionEntity.notifications().stream()
            .map(EventNotificationHandlerConfigEntity::notificationId)
            .map(valueReference -> valueReference.asString(parameters))
            .map(ModelId::of)
            .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.NOTIFICATION_V1))
            .map(entities::get)
            .filter(Objects::nonNull)
            .forEach(notification -> graph.putEdge(entity, notification));
    }

    private void resolveStreamsForInstallation(EntityV1 entity,
                                                     EventDefinitionEntity eventDefinitionEntity,
                                                     Map<String, ValueReference> parameters,
                                                     Map<EntityDescriptor, Entity> entities,
                                                     MutableGraph<Entity> graph) {
        EventProcessorConfigEntity config = eventDefinitionEntity.config();

        if (config instanceof AggregationEventProcessorConfigEntity) {
            AggregationEventProcessorConfigEntity configEntity = (AggregationEventProcessorConfigEntity) config;
            configEntity.streams().stream()
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.STREAM_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(stream -> graph.putEdge(entity, stream));
        }
    }
}
