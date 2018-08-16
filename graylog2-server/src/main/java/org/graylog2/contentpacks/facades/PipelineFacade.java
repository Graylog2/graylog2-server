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
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.contentpacks.exceptions.MissingNativeEntityException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class PipelineFacade implements EntityFacade<PipelineDao> {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.PIPELINE_V1;

    private final ObjectMapper objectMapper;
    private final PipelineService pipelineService;
    private final PipelineStreamConnectionsService connectionsService;
    private final PipelineRuleParser pipelineRuleParser;

    @Inject
    public PipelineFacade(ObjectMapper objectMapper,
                          PipelineService pipelineService,
                          PipelineStreamConnectionsService connectionsService,
                          PipelineRuleParser pipelineRuleParser) {
        this.objectMapper = objectMapper;
        this.pipelineService = pipelineService;
        this.connectionsService = connectionsService;
        this.pipelineRuleParser = pipelineRuleParser;
    }

    @Override
    public EntityWithConstraints exportNativeEntity(PipelineDao pipelineDao) {
        final Set<ValueReference> connectedStreams = connectedStreams(pipelineDao.id());
        final PipelineEntity pipelineEntity = PipelineEntity.create(
                ValueReference.of(pipelineDao.title()),
                ValueReference.of(pipelineDao.description()),
                ValueReference.of(pipelineDao.source()),
                connectedStreams);
        final JsonNode data = objectMapper.convertValue(pipelineEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(pipelineDao.title()))
                .type(ModelTypes.PIPELINE_V1)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    private Set<ValueReference> connectedStreams(String pipelineId) {
        final Set<PipelineConnections> connections = connectionsService.loadByPipelineId(pipelineId);
        return connections.stream()
                .map(PipelineConnections::streamId)
                .map(ValueReference::of)
                .collect(Collectors.toSet());
    }

    @Override
    public NativeEntity<PipelineDao> createNativeEntity(Entity entity,
                                                        Map<String, ValueReference> parameters,
                                                        Map<EntityDescriptor, Object> nativeEntities,
                                                        String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<PipelineDao> decode(EntityV1 entity,
                                             Map<String, ValueReference> parameters,
                                             Map<EntityDescriptor, Object> nativeEntities) {
        final DateTime now = Tools.nowUTC();
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entity.data(), PipelineEntity.class);
        final ValueReference description = pipelineEntity.description();
        final PipelineDao pipelineDao = PipelineDao.builder()
                .title(pipelineEntity.title().asString(parameters))
                .description(description == null ? null : description.asString(parameters))
                .source(pipelineEntity.source().asString(parameters))
                .createdAt(now)
                .modifiedAt(now)
                .build();
        final PipelineDao savedPipelineDao = pipelineService.save(pipelineDao);
        final String pipelineId = requireNonNull(savedPipelineDao.id(), "Saved pipeline ID must not be null");
        final Set<EntityDescriptor> connectedStreamEntities = pipelineEntity.connectedStreams().stream()
                .map(valueReference -> valueReference.asString(parameters))
                .map(streamId -> EntityDescriptor.create(streamId, ModelTypes.STREAM_V1))
                .collect(Collectors.toSet());
        final Set<Stream> connectedStreams = connectedStreams(connectedStreamEntities, nativeEntities);
        createPipelineConnections(pipelineId, connectedStreams);

        return NativeEntity.create(entity.id(), pipelineId, TYPE_V1, savedPipelineDao);
    }

    private Set<Stream> connectedStreams(Set<EntityDescriptor> connectedStreamEntities, Map<EntityDescriptor, Object> nativeEntities) {
        final ImmutableSet.Builder<Stream> streams = ImmutableSet.builder();
        for (EntityDescriptor descriptor : connectedStreamEntities) {
            final Object stream = nativeEntities.get(descriptor);
            if (stream instanceof Stream) {
                streams.add((Stream) stream);
            } else {
                throw new MissingNativeEntityException(descriptor);
            }
        }
        return streams.build();
    }

    private void createPipelineConnections(String pipelineId, Set<Stream> connectedStreams) {
        for (Stream stream : connectedStreams) {
            final String streamId = stream.getId();
            try {
                final PipelineConnections connections = connectionsService.load(streamId);
                final Set<String> newPipelines = ImmutableSet.<String>builder()
                        .addAll(connections.pipelineIds())
                        .add(pipelineId)
                        .build();
                final PipelineConnections newConnections = connections.toBuilder()
                        .pipelineIds(newPipelines)
                        .build();
                final PipelineConnections savedConnections = connectionsService.save(newConnections);
                LOG.trace("Saved pipeline connections: {}", savedConnections);
            } catch (NotFoundException e) {
                final PipelineConnections connections = PipelineConnections.builder()
                        .streamId(streamId)
                        .pipelineIds(Collections.singleton(pipelineId))
                        .build();
                final PipelineConnections savedConnections = connectionsService.save(connections);
                LOG.trace("Saved pipeline connections: {}", savedConnections);
            }
        }
    }

    @Override
    public void delete(PipelineDao nativeEntity) {
        final Set<PipelineConnections> pipelineConnections = connectionsService.loadByPipelineId(nativeEntity.id());
        for (PipelineConnections connections : pipelineConnections) {
            final Set<String> pipelineIds = connections.pipelineIds().stream()
                    .filter(pipelineId -> !pipelineId.equals(nativeEntity.id()))
                    .collect(Collectors.toSet());

            if (pipelineIds.isEmpty()) {
                LOG.trace("Removing pipeline connections for stream {}", connections.streamId());
                connectionsService.delete(connections.streamId());
            } else {
                final PipelineConnections newConnections = connections.toBuilder()
                        .pipelineIds(pipelineIds)
                        .build();
                LOG.trace("Saving updated pipeline connections: {}", newConnections);
                connectionsService.save(newConnections);
            }
        }

        pipelineService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(PipelineDao pipeline) {
        return EntityExcerpt.builder()
                .id(ModelId.of(pipeline.title()))
                .type(ModelTypes.PIPELINE_V1)
                .title(pipeline.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return pipelineService.loadAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final PipelineDao pipelineDao = pipelineService.loadByName(modelId.id());
            return Optional.of(exportNativeEntity(pipelineDao));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline {}", entityDescriptor, e);
            return Optional.empty();
        }
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        try {
            final PipelineDao pipelineDao = pipelineService.loadByName(modelId.id());
            final String pipelineSource = pipelineDao.source();
            final Collection<String> referencedRules = referencedRules(pipelineSource);
            referencedRules.stream()
                    .map(ModelId::of)
                    .map(id -> EntityDescriptor.create(id, ModelTypes.PIPELINE_RULE_V1))
                    .forEach(rule -> mutableGraph.putEdge(entityDescriptor, rule));

            final Set<PipelineConnections> pipelineConnections = connectionsService.loadByPipelineId(pipelineDao.id());
            pipelineConnections.stream()
                    .map(PipelineConnections::streamId)
                    .map(ModelId::of)
                    .map(id -> EntityDescriptor.create(id, ModelTypes.STREAM_V1))
                    .forEach(stream -> mutableGraph.putEdge(entityDescriptor, stream));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline {}", entityDescriptor, e);
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

        final PipelineEntity pipelineEntity = objectMapper.convertValue(entity.data(), PipelineEntity.class);
        final String source = pipelineEntity.source().asString(parameters);
        final Collection<String> referencedRules = referencedRules(source);
        referencedRules.stream()
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.PIPELINE_RULE_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(ruleEntity -> mutableGraph.putEdge(entity, ruleEntity));

        pipelineEntity.connectedStreams().stream()
                .map(valueReference -> valueReference.asString(parameters))
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.STREAM_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(streamEntity -> mutableGraph.putEdge(entity, streamEntity));

        return ImmutableGraph.copyOf(mutableGraph);
    }

    private Collection<String> referencedRules(String pipelineSource) {
        final Pipeline pipeline = pipelineRuleParser.parsePipeline("dummy", pipelineSource);
        return pipeline.stages().stream()
                .map(Stage::ruleReferences)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
