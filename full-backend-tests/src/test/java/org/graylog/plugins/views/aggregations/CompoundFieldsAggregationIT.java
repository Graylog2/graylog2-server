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
package org.graylog.plugins.views.aggregations;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.graylog.testing.completebackend.apis.DefaultStreamMatches;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.apis.inputs.PortBoundGelfInputApi;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.streams.StreamRuleType;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2;
import static org.graylog.testing.containermatrix.SearchServer.OS2_4;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;

@ContainerMatrixTestsConfiguration(searchVersions = {ES7, OS1, OS2, OS2_4, OS2_LATEST})
public class CompoundFieldsAggregationIT {

    private final GraylogApis api;

    public CompoundFieldsAggregationIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeEach
    void setUp() throws ExecutionException, RetryException {
        final String indexSetA = api.indices().createIndexSet("Compound field index A", "Compound field index A", "compound_a");
        final String indexSetB = api.indices().createIndexSet("Compound field index B", "Compound field index B", "compound_b");

        final String streamA = api.streams().createStream("Stream A", indexSetA, DefaultStreamMatches.REMOVE, new Streams.StreamRule(StreamRuleType.EXACT.toInteger(), "streamA", "target_stream", false));
        final String streamB = api.streams().createStream("Stream B", indexSetB, DefaultStreamMatches.REMOVE, new Streams.StreamRule(StreamRuleType.EXACT.toInteger(), "streamB", "target_stream", false));

        final List<String> indexNamesA = waitForIndexNames(indexSetA);
        final List<String> indexNamesB = waitForIndexNames(indexSetB);

        final String indexA = indexNamesA.iterator().next();
        final String indexB = indexNamesB.iterator().next();

        api.backend().searchServerInstance().client().putFieldMapping(indexA, "my_ip", "ip");
        api.backend().searchServerInstance().client().putFieldMapping(indexB, "my_ip", "keyword");

        final PortBoundGelfInputApi gelf = api.gelf().createGelfHttpInput();
        gelf.postMessage("""
                  {"short_message":"compound-field-test-a", "host":"example.org", "_my_ip":"192.168.1.1", "_target_stream": "streamA"}
                """);
        gelf.postMessage("""
                  {"short_message":"compound-field-test-b", "host":"example.org", "_my_ip":"8.8.8.8", "_target_stream": "streamB"}
                """);

        api.search().waitForMessages("compound-field-test-a", "compound-field-test-b");
    }

    private List<String> waitForIndexNames(String indexSetName) throws ExecutionException, RetryException {
        return RetryerBuilder.<List<String>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(30))
                .retryIfResult(List::isEmpty)
                .build()
                .call(() -> api.indices().listOpenIndices(indexSetName).properJSONPath().read("indices.*.index_name"));
    }

    @ContainerMatrixTest
    void aggregate() {
        final GraylogApiResponse responseAsc =
                new GraylogApiResponse(api.post("/search/aggregate","""
                        {
                        	"group_by": [
                        		{
                        			"field": "my_ip"
                        		}
                        	],
                        	"metrics": [
                        		{
                        			"function": "count",
                        			"field": "my_ip",
                        			"sort": "desc"
                        		}
                        	]
                        }
                         """, 200));
    }
}
