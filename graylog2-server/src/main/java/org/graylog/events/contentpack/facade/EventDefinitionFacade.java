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
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EventDefinitionFacade implements EntityFacade<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionFacade.class);

    private final ObjectMapper objectMapper;
    private final EventDefinitionHandler eventDefinitionHandler;
    private final DBEventDefinitionService eventDefinitionService;

    @Inject
    public EventDefinitionFacade(ObjectMapper objectMapper,
                                 EventDefinitionHandler eventDefinitionHandler,
                                 DBEventDefinitionService eventDefinitionService) {
        this.objectMapper = objectMapper;
        this.eventDefinitionHandler = eventDefinitionHandler;
        this.eventDefinitionService = eventDefinitionService;
    }

    @VisibleForTesting
    private Entity exportNativeEntity(EventDefinitionDto eventDefinition, EntityDescriptorIds entityDescriptorIds) {
        final EventDefinitionEntity entity = eventDefinition.toContentPackEntity();

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
        final EventDefinitionDto eventDefinition = eventDefinitionEntity.toNativeEntity(parameters);
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
}
