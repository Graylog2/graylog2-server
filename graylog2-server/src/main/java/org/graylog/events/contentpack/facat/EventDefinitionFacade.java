package org.graylog.events.contentpack.facat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.graylog.events.contentpack.entities.AggregationEventProcessorConfigEntity;
import org.graylog.events.contentpack.entities.AggregationSeriesEntity;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
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
import javax.swing.text.html.Option;

import java.util.EventListener;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EventDefinitionFacade implements EntityFacade<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionFacade.class);

    public static final ModelType TYPE = ModelType.of("event_definition", "1");

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
    Entity exportNativeEntity(EventDefinition eventDefinition, EntityDescriptorIds entityDescriptorIds) {
        final AggregationEventProcessorConfig config = (AggregationEventProcessorConfig) eventDefinition.config();
        final AggregationEventProcessorConfigEntity aggregationEventProcessorConfigEntity = AggregationEventProcessorConfigEntity.builder()
            .type(config.type())
            .query(ValueReference.of(config.query()))
            .streams(config.streams()) // TODO?
            .groupBy(config.groupBy())
            .series(config.series())
            .conditions(config.conditions().orElse(null))
            .executeEveryMs(ValueReference.of(config.executeEveryMs()))
            .searchWithinMs(ValueReference.of(config.searchWithinMs()))
            .build();

        final EventDefinitionEntity entity = EventDefinitionEntity.builder()
                .title(ValueReference.of(eventDefinition.title()))
                .description(ValueReference.of(eventDefinition.description()))
                .priority(ValueReference.of(eventDefinition.priority()))
                .alert(ValueReference.of(eventDefinition.alert()))
                .config(aggregationEventProcessorConfigEntity)
                .notifications(eventDefinition.notifications())
                .notificationSettings(eventDefinition.notificationSettings())
                .fieldSpec(eventDefinition.fieldSpec())
                .keySpec(eventDefinition.keySpec())
                .storage(eventDefinition.storage())
                .build();


        final JsonNode data = objectMapper.convertValue(entity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(eventDefinition.id(), TYPE)))
                .type(TYPE)
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
        final AggregationEventProcessorConfigEntity eventProcessorConfigEntity = (AggregationEventProcessorConfigEntity) eventDefinitionEntity.config();
        final AggregationEventProcessorConfig aggregationEventProcessorConfig = AggregationEventProcessorConfig.builder()
                .type(eventProcessorConfigEntity.type())
                .query(eventProcessorConfigEntity.query().asString(parameters))
                .streams(eventProcessorConfigEntity.streams())
                .groupBy(eventProcessorConfigEntity.groupBy())
                .series(eventProcessorConfigEntity.series())
                .conditions(eventProcessorConfigEntity.conditions().orElse(null))
                .executeEveryMs(eventProcessorConfigEntity.executeEveryMs().asLong(parameters))
                .searchWithinMs(eventProcessorConfigEntity.searchWithinMs().asLong(parameters))
                .build();
        final EventDefinitionDto eventDefinition = EventDefinitionDto.builder()
                .title(eventDefinitionEntity.title().asString(parameters))
                .description(eventDefinitionEntity.description().asString(parameters))
                .priority(eventDefinitionEntity.priority().asInteger(parameters))
                .alert(eventDefinitionEntity.alert().asBoolean(parameters))
                .config(aggregationEventProcessorConfig)
                .fieldSpec(eventDefinitionEntity.fieldSpec())
                .keySpec(eventDefinitionEntity.keySpec())
                .notificationSettings(eventDefinitionEntity.notificationSettings())
                .notifications(eventDefinitionEntity.notifications())
                .storage(eventDefinitionEntity.storage())
                .build();
        final EventDefinitionDto savedDto = eventDefinitionHandler.create(eventDefinition);
        return NativeEntity.create(entity.id(), savedDto.id(), TYPE, savedDto.title(), savedDto);
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
                .type(TYPE)
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
