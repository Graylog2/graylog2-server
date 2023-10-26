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
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.StreamTitleEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class StreamTitleFacade extends StreamFacade {
    private static final Logger LOG = LoggerFactory.getLogger(StreamTitleFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.STREAM_TITLE;

    private final ObjectMapper objectMapper;
    private final StreamService streamService;

    @Inject
    public StreamTitleFacade(ObjectMapper objectMapper, StreamService streamService, StreamRuleService streamRuleService, V20190722150700_LegacyAlertConditionMigration legacyAlertsMigration, IndexSetService indexSetService, UserService userService) {
        super(objectMapper, streamService, streamRuleService, legacyAlertsMigration, indexSetService, userService);
        this.objectMapper = objectMapper;
        this.streamService = streamService;
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(EntityDescriptor.create(entityDescriptor.id().id(), ModelTypes.STREAM_TITLE));
        return ImmutableGraph.copyOf(mutableGraph);
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

    @VisibleForTesting
    Entity exportNativeEntity(Stream stream, EntityDescriptorIds entityDescriptorIds) {
        final StreamTitleEntity streamEntity = StreamTitleEntity.create(ValueReference.of(stream.getTitle()));

        final JsonNode data = objectMapper.convertValue(streamEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(stream.getId(), ModelTypes.STREAM_TITLE)))
                .type(ModelTypes.STREAM_TITLE)
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

    public static Object resolveStreamEntity(String id, Map entities) {
        Object streamEntity = entities.get(EntityDescriptor.create(id, ModelTypes.STREAM_V1));
        if (streamEntity == null) {
            streamEntity = entities.get(EntityDescriptor.create(id, ModelTypes.STREAM_TITLE));
        }
        return streamEntity;
    }

    public static Optional<String> getStreamDescriptor(String id, EntityDescriptorIds entityDescriptorIds) {
        Optional<String> descriptor = entityDescriptorIds.get(id, ModelTypes.STREAM_V1);
        if (!descriptor.isPresent()) {
            descriptor = entityDescriptorIds.get(id, ModelTypes.STREAM_TITLE);
        }
        return descriptor;
    }

    private Optional<NativeEntity<Stream>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final StreamTitleEntity streamEntity = objectMapper.convertValue(entity.data(), StreamTitleEntity.class);
        final Optional<Stream> stream = streamService.loadAll().stream().filter(s -> streamEntity.title().asString().equals(s.getTitle())).findFirst();
        if (stream.isPresent()) {
            return Optional.of(NativeEntity.create(entity.id(), stream.get().getId(), ModelTypes.STREAM_V1, stream.get().getTitle(), stream.get()));
        }
        return Optional.empty();
    }
}
