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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.events.ClusterEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class MongoDbPipelineServiceDeleteTest {

    @Mock
    ClusterEventBus clusterEventBus;

    private MongoDbPipelineService pipelineService;
    private MongoDbPipelineStreamConnectionsService connectionsService;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        final EntityScopeService entityScopeService = new EntityScopeService(
                Set.of(new DefaultEntityScope(), new ImmutableSystemScope()));
        connectionsService = new MongoDbPipelineStreamConnectionsService(mongoCollections, clusterEventBus);
        pipelineService = new MongoDbPipelineService(
                mongoCollections, entityScopeService, clusterEventBus, null, connectionsService);
    }

    @Test
    void deleteRemovesPipelineIdFromConnections() throws NotFoundException {
        final PipelineDao pipelineA = pipelineService.save(PipelineDao.builder()
                .title("pipeline-a")
                .description("a")
                .source("pipeline \"pipeline-a\"\nstage 0 match either\nend")
                .build());
        final PipelineDao pipelineB = pipelineService.save(PipelineDao.builder()
                .title("pipeline-b")
                .description("b")
                .source("pipeline \"pipeline-b\"\nstage 0 match either\nend")
                .build());

        connectionsService.save(PipelineConnections.create(null, "stream-1", Set.of(pipelineA.id(), pipelineB.id())));

        pipelineService.delete(pipelineA.id());

        final PipelineConnections remaining = connectionsService.load("stream-1");
        assertThat(remaining.pipelineIds()).containsExactly(pipelineB.id());
    }

    @Test
    void deleteRemovesConnectionDocumentWhenLastPipeline() {
        final PipelineDao pipeline = pipelineService.save(PipelineDao.builder()
                .title("lonely-pipeline")
                .description("alone")
                .source("pipeline \"lonely-pipeline\"\nstage 0 match either\nend")
                .build());

        connectionsService.save(PipelineConnections.create(null, "stream-2", Set.of(pipeline.id())));

        pipelineService.delete(pipeline.id());

        assertThatThrownBy(() -> connectionsService.load("stream-2"))
                .isInstanceOf(NotFoundException.class);
        assertThat(connectionsService.loadByPipelineId(pipeline.id())).isEmpty();
    }

    @Test
    void deleteDoesNotRemoveConnectionsForNonDeletableSystemPipeline() throws NotFoundException {
        final PipelineDao systemPipeline = pipelineService.save(PipelineDao.create(
                null,
                ImmutableSystemScope.NAME,
                "Default Routing",
                "System generated pipeline",
                "pipeline \"Default Routing\"\nstage 0 match either\nend",
                null,
                null));

        connectionsService.save(PipelineConnections.create(null, "000000000000000000000001", Set.of(systemPipeline.id())));

        assertThatThrownBy(() -> pipelineService.delete(systemPipeline.id()))
                .isInstanceOf(IllegalArgumentException.class);

        final PipelineConnections stillConnected = connectionsService.load("000000000000000000000001");
        assertThat(stillConnected.pipelineIds()).containsExactly(systemPipeline.id());
    }
}
