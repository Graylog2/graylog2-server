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
package org.graylog.pipelines;

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ContainerMatrixTestsConfiguration
public class StreamsPipelinesIT {
    private static final String pipeline1Title = "Test Pipeline 1";
    private static final String pipeline2Title = "Test Pipeline 2";
    private final GraylogApis api;

    private String indexSetId;
    private String stream1Id;
    private String stream2Id;
    private String stream3Id;
    private String pipeline1Id;
    private String pipeline2Id;

    public StreamsPipelinesIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    void beforeAll() {
        this.indexSetId = api.indices().createIndexSet("Test Indices", "Some test indices", "streamstest");
        this.stream1Id = api.streams().createStream("New Stream 1", this.indexSetId);
        this.stream2Id = api.streams().createStream("New Stream 2", this.indexSetId);
        this.stream3Id = api.streams().createStream("New Stream 3", this.indexSetId);
        this.pipeline1Id = api.pipelines().create(pipeline1Title, Set.of(stream1Id, stream2Id));
        this.pipeline2Id = api.pipelines().create(pipeline2Title, Set.of(stream1Id, stream3Id));
    }

    @AfterAll
    void afterAll() {
        api.pipelines().delete(pipeline1Id);
        api.pipelines().delete(pipeline2Id);
        api.streams().deleteStream(this.stream1Id);
        api.streams().deleteStream(this.stream2Id);
        api.streams().deleteStream(this.stream3Id);
        api.indices().deleteIndexSet(this.indexSetId, true);
    }

    @ContainerMatrixTest
    void bulkRetrievalOfPipelineConnections() throws Exception {
        final var result = api.post("/streams/pipelines", Map.of("stream_ids", Set.of(stream1Id, stream2Id, stream3Id)), 200)
                .extract().body().jsonPath();
        final var pipeline1 = Map.of("id", pipeline1Id, "title", pipeline1Title);
        final var pipeline2 = Map.of("id", pipeline2Id, "title", pipeline2Title);

        assertThat(result.getList(stream1Id)).containsExactlyInAnyOrder(pipeline1, pipeline2);
        assertThat(result.getList(stream2Id)).containsExactlyInAnyOrder(pipeline1);
        assertThat(result.getList(stream3Id)).containsExactlyInAnyOrder(pipeline2);
    }

    @ContainerMatrixTest
    void retrievePipelineConnectionsForASingleStream() {
        var result = api.get("/streams/" + stream1Id + "/pipelines", 200)
                .extract().body().jsonPath();

        final var pipeline1 = Map.of("id", pipeline1Id, "title", pipeline1Title);
        final var pipeline2 = Map.of("id", pipeline2Id, "title", pipeline2Title);

        assertThat(result.getList("")).containsExactlyInAnyOrder(pipeline1, pipeline2);
    }
}
