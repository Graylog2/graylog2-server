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

import com.github.rholder.retry.RetryException;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.FullBackendTest;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

@GraylogBackendConfiguration
public class StreamsPipelinesIT {
    private static final String pipeline1Title = "Test Pipeline 1";
    private static final String pipeline2Title = "Test Pipeline 2";

    private static GraylogApis api;

    private static String indexSetId;
    private static String stream1Id;
    private static String stream2Id;
    private static String stream3Id;
    private static String pipeline1Id;
    private static String pipeline2Id;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) throws ExecutionException, RetryException {
        api = graylogApis;
        indexSetId = api.indices().createIndexSet("Test Indices", "Some test indices", "streamstest");
        stream1Id = api.streams().createStream("New Stream 1", indexSetId);
        stream2Id = api.streams().createStream("New Stream 2", indexSetId);
        stream3Id = api.streams().createStream("New Stream 3", indexSetId);
        pipeline1Id = api.pipelines().create(pipeline1Title, Set.of(stream1Id, stream2Id));
        pipeline2Id = api.pipelines().create(pipeline2Title, Set.of(stream1Id, stream3Id));
    }

    @AfterAll
    void afterAll() {
        api.pipelines().delete(pipeline1Id);
        api.pipelines().delete(pipeline2Id);
        api.streams().deleteStream(stream1Id);
        api.streams().deleteStream(stream2Id);
        api.streams().deleteStream(stream3Id);
        api.indices().deleteIndexSet(indexSetId, true);
    }

    private record BulkPipelinesRequest(Collection<String> streamIds) {}

    @FullBackendTest
    void bulkRetrievalOfPipelineConnections() throws Exception {
        final var result = api.post("/streams/pipelines",
                                    new BulkPipelinesRequest(Set.of(stream1Id, stream2Id, stream3Id)),
                                    200)
                              .extract().body().jsonPath();
        final var pipeline1 = pipelineSummary(pipeline1Id, pipeline1Title);
        final var pipeline2 = pipelineSummary(pipeline2Id, pipeline2Title);

        assertThat(result.getList(stream1Id)).containsExactlyInAnyOrder(pipeline1, pipeline2);
        assertThat(result.getList(stream2Id)).containsExactlyInAnyOrder(pipeline1);
        assertThat(result.getList(stream3Id)).containsExactlyInAnyOrder(pipeline2);
    }

    @FullBackendTest
    void bulkRetrievalOfPipelineConnectionsForBuiltinStreams() throws Exception {
        final var result = api.post("/streams/pipelines",
                                    new BulkPipelinesRequest(Set.of(DEFAULT_STREAM_ID,
                                                                    DEFAULT_EVENTS_STREAM_ID,
                                                                    DEFAULT_SYSTEM_EVENTS_STREAM_ID)),
                                    200)
                              .extract().body().jsonPath();

        assertThat(result.getList(DEFAULT_STREAM_ID)).isEmpty();
        assertThat(result.getList(DEFAULT_EVENTS_STREAM_ID)).isEmpty();
        assertThat(result.getList(DEFAULT_SYSTEM_EVENTS_STREAM_ID)).isEmpty();
    }

    @FullBackendTest
    void bulkRetrievalOfPipelineConnectionsForDanglingReferences() throws Exception {
        final var defaultIndexSet = api.indices().defaultIndexSetId();
        final var streamId = api.streams().createStream("Stream with dangling pipeline reference", defaultIndexSet);
        final var pipelineId = api.pipelines().create("A pipeline which is about to get deleted", Set.of(streamId));
        api.pipelines().delete(pipelineId);

        final var result = api.post("/streams/pipelines", new BulkPipelinesRequest(Set.of(streamId)), 200)
                              .extract().body().jsonPath();

        assertThat(result.getList(streamId)).isEmpty();
    }

    @FullBackendTest
    void retrievePipelineConnectionsForASingleStream() {
        var result = api.get("/streams/" + stream1Id + "/pipelines", 200)
                        .extract().body().jsonPath();

        final var pipeline1 = pipelineSummary(pipeline1Id, pipeline1Title);
        final var pipeline2 = pipelineSummary(pipeline2Id, pipeline2Title);

        assertThat(result.getList("")).containsExactlyInAnyOrder(pipeline1, pipeline2);
    }

    private Map<String, String> pipelineSummary(String id, String title) {
        return Map.of("id", id, "title", title);
    }
}
