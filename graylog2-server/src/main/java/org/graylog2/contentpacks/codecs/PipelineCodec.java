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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineCodec implements EntityCodec<PipelineDao> {
    private final ObjectMapper objectMapper;
    private final PipelineService pipelineService;
    private final PipelineStreamConnectionsService connectionsService;

    @Inject
    public PipelineCodec(ObjectMapper objectMapper,
                         PipelineService pipelineService,
                         PipelineStreamConnectionsService connectionsService) {
        this.objectMapper = objectMapper;
        this.pipelineService = pipelineService;
        this.connectionsService = connectionsService;
    }

    @Override
    public Entity encode(PipelineDao pipelineDao) {
        final Set<String> connectedStreams = connectedStreams(pipelineDao.id());
        final PipelineEntity pipelineEntity = PipelineEntity.create(
                pipelineDao.title(),
                pipelineDao.description(),
                pipelineDao.source(),
                connectedStreams);
        final JsonNode data = objectMapper.convertValue(pipelineEntity, JsonNode.class);

        return EntityV1.builder()
                .id(ModelId.of(pipelineDao.title()))
                .type(ModelTypes.PIPELINE)
                .data(data)
                .build();
    }

    private Set<String> connectedStreams(String pipelineId) {
        final Set<PipelineConnections> connections = connectionsService.loadByPipelineId(pipelineId);
        return connections.stream()
                .map(PipelineConnections::streamId)
                .collect(Collectors.toSet());
    }

    @Override
    public PipelineDao decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private PipelineDao decodeEntityV1(EntityV1 entity) {
        final DateTime now = Tools.nowUTC();
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entity.data(), PipelineEntity.class);
        final PipelineDao pipelineDao = PipelineDao.builder()
                .title(pipelineEntity.title())
                .description(pipelineEntity.description())
                .source(pipelineEntity.source())
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
}
