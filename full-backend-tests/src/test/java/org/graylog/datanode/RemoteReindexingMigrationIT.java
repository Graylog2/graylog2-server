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

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CloseIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateIndexResponse;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.storage.opensearch2.testing.OpenSearchInstanceBuilder;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.elasticsearch.IndexState;
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
    void testRemoteAsyncReindexing() throws ExecutionException, RetryException {

        final String indexName = createRandomSourceIndex();
        final String indexName2 = createRandomSourceIndex();

        final String messageContent = ingestRandomMessage(indexName);
        final String messageContent2 = ingestRandomMessage(indexName2);

        closeSourceIndex(indexName);

        createTargetIndex(indexName, true);
        createTargetIndex(indexName2, false);
        blockTargetIndex(indexName2);

        Assertions.assertThat(getTargetIndexState(indexName))
                .isEqualTo(IndexState.CLOSE);

        // flush the newly created document
        openSearchInstance.client().refreshNode();

        final String request = """
                {
                    "allowlist": "%s",
                    "hostname": "%s",
                    "indices": ["%s", "%s"],
                    "synchronous": false
                }
                """.formatted(openSearchInstance.internalUri(), openSearchInstance.internalUri(), indexName, indexName2);


        final ValidatableResponse migrationResponse = apis.post("/remote-reindex-migration/remoteReindex", request, 200);
        final String migrationID = migrationResponse.extract().body().asString();

        ValidatableResponse response = waitForMigrationFinished(migrationID);

        final String status = response.extract().body().jsonPath().get("status");
        Assertions.assertThat(status).isEqualTo("FINISHED");

        Assertions.assertThat(getTargetIndexState(indexName))
                        .isEqualTo(IndexState.CLOSE);


        openTargetIndex(indexName);

        Assertions.assertThat(waitForMessage(indexName, messageContent)).containsEntry("message", messageContent);
        Assertions.assertThat(waitForMessage(indexName2, messageContent2)).containsEntry("message", messageContent2);


    }

    private void blockTargetIndex(String indexName) {
        apis.backend().searchServerInstance().client().setIndexBlock(indexName);
    }

    private void openTargetIndex(String indexName) {
        apis.backend().searchServerInstance().client().openIndex(indexName);
    }

    private IndexState getTargetIndexState(String indexName) {
        return apis.backend().searchServerInstance().client().getStatus(indexName);
    }

    private void createTargetIndex(String indexName, boolean closed) {
        apis.backend().searchServerInstance().client().createIndex(indexName);
        if (closed) {
            apis.backend().searchServerInstance().client().closeIndex(indexName);
        }
    }

    private void closeSourceIndex(String indexName) {
        openSearchInstance.openSearchClient().execute((restHighLevelClient, requestOptions) -> restHighLevelClient.indices().close(new CloseIndexRequest(indexName), requestOptions));
    }

    /**
     * @return name of the newly created index
     */
    private String createRandomSourceIndex() {
        String indexName = RandomStringUtils.randomAlphanumeric(15).toLowerCase(Locale.ROOT);
        final CreateIndexResponse response = openSearchInstance.openSearchClient().execute((restHighLevelClient, requestOptions) -> restHighLevelClient.indices().create(new CreateIndexRequest(indexName), requestOptions));
        return response.index();
    }

    /**
     * @return content of the created message. Useful for later verification that the message has been successfully
     * transferred from old to new cluster.
     */
    private String ingestRandomMessage(String indexName) {
        String messageContent = RandomStringUtils.randomAlphanumeric(20);
        final IndexResponse response = openSearchInstance.openSearchClient().execute((restHighLevelClient, requestOptions) -> {
            final IndexRequest req = new IndexRequest();
            req.index(indexName);
            req.source(Map.of("message", messageContent));
            return restHighLevelClient.index(req, requestOptions);
        });
        return messageContent;
    }

    private ValidatableResponse waitForMigrationFinished(String migrationID) throws ExecutionException, RetryException {
        return RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfResult(r -> !r.extract().body().jsonPath().get("status").equals("FINISHED"))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasResult()) {
                            final String status = ((ValidatableResponse) attempt.getResult()).extract().body().asString();
                            LOG.info("Current reindex status: " + status);
                        }
                    }
                }).build()
                .call(() -> apis.get("/remote-reindex-migration/status/" + migrationID, 200));
    }

    private Map<String, Object> waitForMessage(String indexName, String messageContent) throws ExecutionException, RetryException {
        return RetryerBuilder.<Optional<Map<String, Object>>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfResult(Optional::isEmpty)
                .build().call(() -> apis.backend().searchServerInstance().client().findMessage(indexName, "message:" + messageContent))
                .orElseThrow(() -> new IllegalStateException("Message should be present!"));
    }
}
