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
package org.graylog2.contentpacks.catalogs;

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
import org.graylog2.contentpacks.codecs.PipelineCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.database.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineCatalog implements EntityCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineCatalog.class);

    public static final ModelType TYPE = ModelTypes.PIPELINE;

    private final PipelineService pipelineService;
    private final PipelineStreamConnectionsService streamConnectionsService;
    private final PipelineRuleParser pipelineRuleParser;
    private final PipelineCodec codec;

    @Inject
    public PipelineCatalog(PipelineService pipelineService,
                           PipelineStreamConnectionsService streamConnectionsService,
                           PipelineRuleParser pipelineRuleParser,
                           PipelineCodec codec) {
        this.pipelineService = pipelineService;
        this.streamConnectionsService = streamConnectionsService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.codec = codec;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return pipelineService.loadAll().stream()
                .map(codec::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final PipelineDao pipelineDao = pipelineService.loadByName(modelId.id());
            return Optional.of(codec.encode(pipelineDao));
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

            final Set<PipelineConnections> pipelineConnections = streamConnectionsService.loadByPipelineId(pipelineDao.id());
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
