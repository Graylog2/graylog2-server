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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.bson.types.ObjectId;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.StreamAlarmCallbackEntity;
import org.graylog2.contentpacks.model.entities.StreamAlertConditionEntity;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.StreamRuleEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.rest.models.streams.alerts.requests.CreateConditionRequest;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.nullToEmpty;

public class StreamFacade implements EntityFacade<Stream> {
    private static final Logger LOG = LoggerFactory.getLogger(StreamFacade.class);
    private static final String DUMMY_STREAM_ID = "ffffffffffffffffffffffff";

    public static final ModelType TYPE_V1 = ModelTypes.STREAM_V1;

    private final ObjectMapper objectMapper;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final AlertService streamAlertService;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final Set<PluginMetaData> pluginMetaData;
    private final IndexSetService indexSetService;

    @Inject
    public StreamFacade(ObjectMapper objectMapper,
                        StreamService streamService,
                        StreamRuleService streamRuleService,
                        AlertService streamAlertService,
                        AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                        Set<PluginMetaData> pluginMetaData,
                        IndexSetService indexSetService) {
        this.objectMapper = objectMapper;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.streamAlertService = streamAlertService;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.pluginMetaData = pluginMetaData;
        this.indexSetService = indexSetService;
    }

    @VisibleForTesting
    Entity exportNativeEntity(Stream stream, EntityDescriptorIds entityDescriptorIds) {
        final List<StreamRuleEntity> streamRules = stream.getStreamRules().stream()
                .map(this::encodeStreamRule)
                .collect(Collectors.toList());
        final List<AlertCondition> alertConditions = streamService.getAlertConditions(stream);
        final List<StreamAlertConditionEntity> streamAlertConditions = alertConditions.stream()
                .map(this::encodeStreamAlertCondition)
                .collect(Collectors.toList());
        final List<StreamAlarmCallbackEntity> streamAlarmCallbacks = alarmCallbackConfigurationService.getForStream(stream).stream()
                .map(this::encodeStreamAlarmCallback)
                .collect(Collectors.toList());
        final Set<ValueReference> outputIds = stream.getOutputs().stream()
                .map(output -> entityDescriptorIds.getOrThrow(output.getId(), ModelTypes.OUTPUT_V1))
                .map(ValueReference::of)
                .collect(Collectors.toSet());
        final StreamEntity streamEntity = StreamEntity.create(
                ValueReference.of(stream.getTitle()),
                ValueReference.of(stream.getDescription()),
                ValueReference.of(stream.getDisabled()),
                ValueReference.of(stream.getMatchingType()),
                streamRules,
                streamAlertConditions,
                streamAlarmCallbacks,
                outputIds,
                ValueReference.of(stream.isDefaultStream()),
                ValueReference.of(stream.getRemoveMatchesFromDefaultStream()));

        final JsonNode data = objectMapper.convertValue(streamEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(stream.getId(), ModelTypes.STREAM_V1)))
                .type(ModelTypes.STREAM_V1)
                .constraints(versionConstraints(streamAlarmCallbacks, alertConditions))
                .data(data)
                .build();
    }

    private ImmutableSet<Constraint> versionConstraints(List<StreamAlarmCallbackEntity> alarmCallbacks,
                                                        List<AlertCondition> streamAlertConditions) {
        // Try to collect plugin dependencies by looking that the package names of the alarm callbacks/conditions and
        // the loaded plugins
        final java.util.stream.Stream<String> concat = java.util.stream.Stream.concat(
                alarmCallbacks.stream().map(StreamAlarmCallbackEntity::type),
                streamAlertConditions.stream().map(condition -> condition.getClass().getCanonicalName())

        );
        return concat.flatMap(packageName -> pluginMetaData.stream()
                .filter(metaData -> packageName.startsWith(metaData.getClass().getPackage().getName()))
                .map(PluginVersionConstraint::of))
                .collect(ImmutableSet.toImmutableSet());
    }

    private StreamRuleEntity encodeStreamRule(StreamRule streamRule) {
        return StreamRuleEntity.create(
                ValueReference.of(streamRule.getType()),
                ValueReference.of(streamRule.getField()),
                ValueReference.of(nullToEmpty(streamRule.getValue())), // Rule value can be null!
                ValueReference.of(streamRule.getInverted()),
                ValueReference.of(nullToEmpty(streamRule.getDescription()))); // Rule description can be null!
    }

    private StreamAlertConditionEntity encodeStreamAlertCondition(AlertCondition alertCondition) {
        return StreamAlertConditionEntity.create(
                alertCondition.getType(),
                ValueReference.of(alertCondition.getTitle()),
                ReferenceMapUtils.toReferenceMap(alertCondition.getParameters()));
    }

    private StreamAlarmCallbackEntity encodeStreamAlarmCallback(AlarmCallbackConfiguration alarmCallback) {
        return StreamAlarmCallbackEntity.create(
                alarmCallback.getType(),
                ValueReference.of(alarmCallback.getTitle()),
                alarmCallback.getStreamId(),
                ReferenceMapUtils.toReferenceMap(alarmCallback.getConfiguration()));
    }

    @Override
    public NativeEntity<Stream> createNativeEntity(Entity entity,
                                                   Map<String, ValueReference> parameters,
                                                   Map<EntityDescriptor, Object> nativeEntities,
                                                   String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities, username);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<Stream> decode(EntityV1 entity,
                                        Map<String, ValueReference> parameters,
                                        Map<EntityDescriptor, Object> nativeEntities,
                                        String username) {
        final StreamEntity streamEntity = objectMapper.convertValue(entity.data(), StreamEntity.class);
        final CreateStreamRequest createStreamRequest = CreateStreamRequest.create(
                streamEntity.title().asString(parameters),
                streamEntity.description().asString(parameters),
                null, // ignored
                null,
                streamEntity.matchingType().asString(parameters),
                streamEntity.removeMatches().asBoolean(parameters),
                indexSetService.getDefault().id());
        final Stream stream = streamService.create(createStreamRequest, username);
        final List<StreamRule> streamRules = streamEntity.streamRules().stream()
                .map(streamRuleEntity -> createStreamRuleRequest(streamRuleEntity, parameters))
                .map(request -> streamRuleService.create(DUMMY_STREAM_ID, request))
                .collect(Collectors.toList());
        final List<AlertCondition> alertConditions = streamEntity.alertConditions().stream()
                .map(alertCondition -> createStreamAlertConditionRequest(alertCondition, parameters))
                .map(request -> {
                    try {
                        return streamAlertService.fromRequest(request, stream, username);
                    } catch (ConfigurationException e) {
                        throw new ContentPackException("Couldn't create entity " + entity.toEntityDescriptor(), e);
                    }
                })
                .collect(Collectors.toList());
        final List<AlarmCallbackConfiguration> alarmCallbacks = streamEntity.alarmCallbacks().stream()
                .map(alarmCallback -> createStreamAlarmCallbackRequest(alarmCallback, parameters))
                .map(request -> alarmCallbackConfigurationService.create(stream.getId(), request, username))
                .collect(Collectors.toList());
        final String savedStreamId;
        try {
            savedStreamId = streamService.saveWithRules(stream, streamRules);

            for (final AlertCondition alertCondition : alertConditions) {
                streamService.addAlertCondition(stream, alertCondition);
            }
            for (final AlarmCallbackConfiguration alarmCallback : alarmCallbacks) {
                alarmCallbackConfigurationService.save(alarmCallback);
            }
        } catch (ValidationException e) {
            throw new ContentPackException("Couldn't create entity " + entity.toEntityDescriptor(), e);
        }

        final Set<ObjectId> outputIds = streamEntity.outputs().stream()
                .map(valueReference -> valueReference.asString(parameters))
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.OUTPUT_V1))
                .map(descriptor -> findOutput(descriptor, nativeEntities))
                .map(Output::getId)
                .map(ObjectId::new)
                .collect(Collectors.toSet());
        streamService.addOutputs(new ObjectId(savedStreamId), outputIds);

        return NativeEntity.create(entity.id(), savedStreamId, TYPE_V1, stream.getTitle(), stream);
    }

    private CreateConditionRequest createStreamAlertConditionRequest(StreamAlertConditionEntity alertCondition,
                                                                     Map<String, ValueReference> parameters) {
        return CreateConditionRequest.builder()
                .setType(alertCondition.type())
                .setTitle(alertCondition.title().asString(parameters))
                .setParameters(ReferenceMapUtils.toValueMap(alertCondition.parameters(), parameters))
                .build();
    }

    private CreateAlarmCallbackRequest createStreamAlarmCallbackRequest(StreamAlarmCallbackEntity alarmCallback,
                                                                        Map<String, ValueReference> parameters) {
        return CreateAlarmCallbackRequest.create(
                alarmCallback.type(),
                alarmCallback.title().asString(parameters),
                ReferenceMapUtils.toValueMap(alarmCallback.configuration(), parameters));
    }


    private CreateStreamRuleRequest createStreamRuleRequest(StreamRuleEntity streamRuleEntity, Map<String, ValueReference> parameters) {
        return CreateStreamRuleRequest.create(
                streamRuleEntity.type().asEnum(parameters, StreamRuleType.class).getValue(),
                streamRuleEntity.value().asString(parameters),
                streamRuleEntity.field().asString(parameters),
                streamRuleEntity.inverted().asBoolean(parameters),
                streamRuleEntity.description().asString(parameters));
    }

    private Output findOutput(EntityDescriptor outputDescriptor, Map<EntityDescriptor, Object> nativeEntities) {
        final Object output = nativeEntities.get(outputDescriptor);
        if (output == null) {
            throw new ContentPackException("Missing referenced output: " + outputDescriptor);
        } else if (output instanceof Output) {
            return (Output) output;
        } else {
            final String msg = "Invalid entity type for referenced output " + outputDescriptor + ": " + output.getClass();
            throw new ContentPackException(msg);
        }
    }

    @Override
    public Optional<NativeEntity<Stream>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<Stream>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final String streamId = entity.id().id();
        // Always use the existing default stream
        if (Stream.DEFAULT_STREAM_ID.equals(streamId)) {
            try {
                final Stream stream = streamService.load(Stream.DEFAULT_STREAM_ID);
                return Optional.of(NativeEntity.create(entity.id(), Stream.DEFAULT_STREAM_ID, ModelTypes.STREAM_V1, stream.getTitle(), stream));
            } catch (NotFoundException e) {
                throw new ContentPackException("Default stream <" + streamId + "> does not exist!", e);
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<NativeEntity<Stream>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        try {
            final Stream stream = streamService.load(nativeEntityDescriptor.id().id());
            return Optional.of(NativeEntity.create(nativeEntityDescriptor, stream));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(Stream nativeEntity) {
        if (nativeEntity.isDefaultStream()) {
            LOG.debug("The default stream should not be deleted");
            return;
        }
        try {
            streamService.destroy(nativeEntity);
        } catch (NotFoundException ignore) {
        }
    }

    @Override
    public EntityExcerpt createExcerpt(Stream stream) {
        return EntityExcerpt.builder()
                .id(ModelId.of(stream.getId()))
                .type(ModelTypes.STREAM_V1)
                .title(stream.getTitle())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return streamService.loadAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final Stream stream = streamService.load(modelId.id());
            return Optional.of(exportNativeEntity(stream, entityDescriptorIds));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find stream {}", entityDescriptor, e);
            return Optional.empty();
        }
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        try {
            final Stream stream = streamService.load(modelId.id());
            stream.getOutputs().stream()
                    .map(Output::getId)
                    .map(ModelId::of)
                    .map(id -> EntityDescriptor.create(id, ModelTypes.OUTPUT_V1))
                    .forEach(output -> mutableGraph.putEdge(entityDescriptor, output));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find stream {}", entityDescriptor, e);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveForInstallation((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveForInstallation(EntityV1 entity,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final StreamEntity streamEntity = objectMapper.convertValue(entity.data(), StreamEntity.class);

        streamEntity.outputs().stream()
                .map(valueReference -> valueReference.asString(parameters))
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.OUTPUT_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(outputEntity -> mutableGraph.putEdge(entity, outputEntity));

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
