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
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineFacade implements EntityFacade<PipelineDao> {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineFacade.class);

    public static final ModelType TYPE = ModelTypes.PIPELINE;

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
    public EntityWithConstraints encode(PipelineDao pipelineDao) {
        final Set<ValueReference> connectedStreams = connectedStreams(pipelineDao.id());
        final PipelineEntity pipelineEntity = PipelineEntity.create(
                ValueReference.of(pipelineDao.title()),
                ValueReference.of(pipelineDao.description()),
                ValueReference.of(pipelineDao.source()),
                connectedStreams);
        final JsonNode data = objectMapper.convertValue(pipelineEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(pipelineDao.title()))
                .type(ModelTypes.PIPELINE)
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
    public PipelineDao decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private PipelineDao decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters) {
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

        // TODO: Create pipeline-stream connections

        return pipelineService.save(pipelineDao);
    }

    @Override
    public EntityExcerpt createExcerpt(PipelineDao pipeline) {
        return EntityExcerpt.builder()
                .id(ModelId.of(pipeline.title()))
                .type(ModelTypes.PIPELINE)
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
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final PipelineDao pipelineDao = pipelineService.loadByName(modelId.id());
            return Optional.of(encode(pipelineDao));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline {}", entityDescriptor, e);
            return Optional.empty();
        }
    }

    @Override
    public Graph<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        try {
            final PipelineDao pipelineDao = pipelineService.loadByName(modelId.id());
            final String pipelineSource = pipelineDao.source();
            final Collection<String> referencedRules = referencedRules(pipelineSource);
            referencedRules.stream()
                    .map(ModelId::of)
                    .map(id -> EntityDescriptor.create(id, ModelTypes.PIPELINE_RULE))
                    .forEach(output -> mutableGraph.putEdge(entityDescriptor, output));

            final Set<PipelineConnections> pipelineConnections = connectionsService.loadByPipelineId(pipelineDao.id());
            pipelineConnections.stream()
                    .map(PipelineConnections::streamId)
                    .map(ModelId::of)
                    .map(id -> EntityDescriptor.create(id, ModelTypes.STREAM))
                    .forEach(output -> mutableGraph.putEdge(entityDescriptor, output));
        } catch (NotFoundException e) {
            LOG.debug("Couldn't find pipeline {}", entityDescriptor, e);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }

    private Collection<String> referencedRules(String pipelineSource) {
        final Pipeline pipeline = pipelineRuleParser.parsePipeline("dummy", pipelineSource);
        return pipeline.stages().stream()
                .map(Stage::getRules)
                .flatMap(Collection::stream)
                .map(Rule::name)
                .collect(Collectors.toSet());
    }
}
