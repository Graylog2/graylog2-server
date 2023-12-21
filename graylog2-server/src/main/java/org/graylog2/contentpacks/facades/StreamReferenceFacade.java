/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.events.legacy.V20190722150700_LegacyAlertConditionMigration;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.StreamReferenceEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamReferenceFacade extends StreamFacade {
    private static final Logger LOG = LoggerFactory.getLogger(StreamReferenceFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.STREAM_REF_V1;

    private final ObjectMapper objectMapper;
    private final StreamService streamService;

    @Inject
    public StreamReferenceFacade(ObjectMapper objectMapper, StreamService streamService, StreamRuleService streamRuleService, V20190722150700_LegacyAlertConditionMigration legacyAlertsMigration, IndexSetService indexSetService, UserService userService) {
        super(objectMapper, streamService, streamRuleService, legacyAlertsMigration, indexSetService, userService);
        this.objectMapper = objectMapper;
        this.streamService = streamService;
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(EntityDescriptor.create(entityDescriptor.id().id(), ModelTypes.STREAM_REF_V1));
        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();

        // If we are already exporting the actual Stream, we don't need to export the title entity too.
        if (entityDescriptorIds.get(modelId.id(), ModelTypes.STREAM_V1).isPresent()) {
            return Optional.empty();
        }

        try {
            final Stream stream = streamService.load(modelId.id());
            return Optional.of(exportNativeEntity(stream, entityDescriptorIds));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find stream {}", entityDescriptor, e);
            return Optional.empty();
        }
    }

    @VisibleForTesting
    Entity exportNativeEntity(Stream stream, EntityDescriptorIds entityDescriptorIds) {
        final StreamReferenceEntity streamEntity = StreamReferenceEntity.create(ValueReference.of(stream.getTitle()));

        final JsonNode data = objectMapper.convertValue(streamEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(stream.getId(), ModelTypes.STREAM_REF_V1)))
                .type(ModelTypes.STREAM_REF_V1)
                .data(data)
                .build();
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
            mutableGraph.addNode(entity);
            return ImmutableGraph.copyOf(mutableGraph);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
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

    @Override
    public NativeEntity<Stream> createNativeEntity(Entity entity,
                                                   Map<String, ValueReference> parameters,
                                                   Map<EntityDescriptor, Object> nativeEntities,
                                                   String username) {
        if (entity instanceof EntityV1) {
            // Throw an exception if this is reached. Install process should fail earlier if no existing Stream found.
            // A native entity cannot be created as this is only a reference to an existing stream.
            final StreamReferenceEntity streamEntity = objectMapper.convertValue(((EntityV1) entity).data(), StreamReferenceEntity.class);
            throw new ContentPackException("Stream with title <" + streamEntity.title().asString(parameters) + "> does not exist!");
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<Stream>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final StreamReferenceEntity streamEntity = objectMapper.convertValue(entity.data(), StreamReferenceEntity.class);
        final List<Stream> streams = streamService.loadAllByTitle(streamEntity.title().asString());
        if (streams.size() == 1) {
            final Stream stream = streams.get(0);
            return Optional.of(NativeEntity.create(entity.id(), stream.getId(), ModelTypes.STREAM_V1, stream.getTitle(), stream));
        } else {
            throw new ContentPackException(streams.isEmpty()
                    ? "Stream with title <" + streamEntity.title().asString(parameters) + "> does not exist!"
                    : "Multiple Streams with title <" + streamEntity.title().asString(parameters) + "> exist!");
        }
    }

    @Override
    public EntityExcerpt createExcerpt(Stream stream) {
        return EntityExcerpt.builder()
                .id(ModelId.of(stream.getTitle()))
                .type(ModelTypes.STREAM_REF_V1)
                .title(stream.getTitle())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return streamService.loadAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    public static Entity resolveStreamEntity(String id, Map<EntityDescriptor, Entity> entities) {
        return (Entity) resolveStreamEntityObject(id, entities);
    }

    public static Object resolveStreamEntityObject(String id, Map entities) {
        Object streamEntity = entities.get(EntityDescriptor.create(id, ModelTypes.STREAM_V1));
        if (streamEntity == null) {
            streamEntity = entities.get(EntityDescriptor.create(id, ModelTypes.STREAM_REF_V1));
        }
        return streamEntity;
    }

    public static Optional<String> getStreamEntityId(String id, EntityDescriptorIds entityDescriptorIds) {
        Optional<String> descriptor = entityDescriptorIds.get(id, ModelTypes.STREAM_V1);
        if (descriptor.isEmpty()) {
            descriptor = entityDescriptorIds.get(id, ModelTypes.STREAM_REF_V1);
        }
        return descriptor;
    }

    public static String getStreamEntityIdOrThrow(String id, EntityDescriptorIds entityDescriptorIds) {
        return getStreamEntityId(id, entityDescriptorIds).orElseThrow(() ->
                new ContentPackException("Couldn't find entity " + id + "/" + ModelTypes.STREAM_V1 + " or " + ModelTypes.STREAM_REF_V1));
    }
}
