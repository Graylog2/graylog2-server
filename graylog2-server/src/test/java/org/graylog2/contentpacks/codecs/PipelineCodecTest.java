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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private PipelineService pipelineService;
    private PipelineStreamConnectionsService connectionsService;
    private PipelineCodec codec;

    @Before
    public void setUp() {
        connectionsService = new InMemoryPipelineStreamConnectionsService();
        codec = new PipelineCodec(objectMapper, pipelineService, connectionsService);
    }

    @Test
    public void encode() {
        final PipelineDao pipeline = PipelineDao.builder()
                .id("pipeline-1234")
                .title("title")
                .description("description")
                .source("pipeline \"Test\"\nstage 0 match either\nrule \"debug\"\nend")
                .build();
        final PipelineConnections connections = PipelineConnections.create("id", "stream-1234", Collections.singleton("pipeline-1234"));
        connectionsService.save(connections);

        final Entity entity = codec.encode(pipeline);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("title"));
        assertThat(entity.type()).isEqualTo(ModelType.of("pipeline"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo("title");
        assertThat(pipelineEntity.description()).isEqualTo("description");
        assertThat(pipelineEntity.source()).startsWith("pipeline \"Test\"");
        assertThat(pipelineEntity.connectedStreams()).containsOnly("stream-1234");
    }

    @Test
    public void createExcerpt() {
        final PipelineDao pipeline = PipelineDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("pipeline \"Test\"\nstage 0 match either\nrule \"debug\"\nend")
                .build();
        final EntityExcerpt excerpt = codec.createExcerpt(pipeline);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("title"));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("pipeline"));
        assertThat(excerpt.title()).isEqualTo("title");
    }
}