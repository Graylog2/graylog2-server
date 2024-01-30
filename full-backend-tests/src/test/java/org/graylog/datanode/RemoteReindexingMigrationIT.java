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
package org.graylog.datanode;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateIndexResponse;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.storage.opensearch2.testing.OpenSearchInstanceBuilder;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.DATANODE_DEV)
public class RemoteReindexingMigrationIT {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteReindexingMigrationIT.class);

    private GraylogApis apis;
    private OpenSearchInstance openSearchInstance;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
        openSearchInstance = OpenSearchInstanceBuilder.builder()
                .network(apis.backend().network())
                .hostname("existing-opensearch-cluster")
                .build();
    }

    @AfterEach
    void tearDown() {
        this.openSearchInstance.close();
    }

    @ContainerMatrixTest
    void testRemoteReindexing() throws ExecutionException, RetryException {

        final String indexName = RandomStringUtils.randomAlphanumeric(15).toLowerCase(Locale.ROOT);

        final String messageContent = RandomStringUtils.randomAlphanumeric(20);

        final CreateIndexResponse indexCreationResponse = openSearchInstance.openSearchClient().execute((restHighLevelClient, requestOptions) -> restHighLevelClient.indices().create(new CreateIndexRequest(indexName), requestOptions));
        LOG.info("Index {} created in old Opensearch cluster", indexCreationResponse.index());

        final IndexResponse response = openSearchInstance.openSearchClient().execute((restHighLevelClient, requestOptions) -> {
            final IndexRequest req = new IndexRequest();
            req.index(indexName);
            req.source(Map.of("message", messageContent));
            return restHighLevelClient.index(req, requestOptions);
        });


        LOG.info("Document indexed: " + response);

        // flush the newly created document
        openSearchInstance.client().refreshNode();

        final String request = """
                {
                    "hostname": "%s",
                    "indices": ["%s"]
                }
                """.formatted(openSearchInstance.internalUri(), indexName);


        LOG.info("Requesting remote reindex: " + request);

        final ValidatableResponse migrationResponse = apis.post("/remote-reindex-migration/remoteReindex", request, 200);

        // one document migrated
        migrationResponse.assertThat().body("results.created", Matchers.hasSize(1));
        migrationResponse.assertThat().body("results.created", Matchers.contains(1));

        /*

        ValidatableResponse status = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfResult(r -> !r.extract().body().asString().equals("FINISHED"))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasResult()) {
                            final String status = ((ValidatableResponse) attempt.getResult()).extract().body().asString();
                            LOG.info("Current reindex status: " + status);
                        }
                    }
                }).build()
                .call(() -> apis.get("/migration/remoteReindex", 200));

        Assertions.assertThat(status).isEqualTo("FINISHED");

         */

        final Optional<Map<String, Object>> transferedMessage = RetryerBuilder.<Optional<Map<String, Object>>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfResult(Optional::isEmpty)
                .build().call(() -> apis.backend().searchServerInstance().client().findMessage(indexName, "message:" + messageContent));

        Assertions.assertThat(transferedMessage)
                .isPresent()
                .hasValueSatisfying(message -> Assertions.assertThat(message.get("message")).isEqualTo(messageContent));
    }
}
