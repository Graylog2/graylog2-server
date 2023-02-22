package org.graylog.datanode.integration;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.http.NoHttpResponseException;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DatanodeClusterIT {

    private DatanodeContainerizedBackend primaryNode;
    private DatanodeContainerizedBackend secondaryNode;

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeClusterIT.class);

    @BeforeEach
    void setUp() {
        primaryNode = new DatanodeContainerizedBackend().start();

        secondaryNode = new DatanodeContainerizedBackend(
                primaryNode.getNetwork(),
                primaryNode.getMongodbContainer(),
                "node2"
        );
        secondaryNode.start();
    }

    @AfterEach
    void tearDown() {
        secondaryNode.stop();
        primaryNode.stop();
    }

    @Test
    void testClusterFormation() throws ExecutionException, RetryException {

        try {
            final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                    .retryIfException(input -> input instanceof NoHttpResponseException)
                    .retryIfException(input -> input instanceof SocketException)
                    .retryIfResult(input -> !input.extract().body().path("number_of_nodes").equals(2))
                    .build();

            final Integer opensearchPort = primaryNode.getOpensearchRestPort();

            retryer.call(() -> this.getStatus(opensearchPort))
                    .assertThat()
                    .body("status", Matchers.equalTo("green"))
                    .body("number_of_nodes", Matchers.equalTo(2))
                    .body("discovered_cluster_manager", Matchers.equalTo(true));
        } catch (RetryException retryException) {
            LOG.error("DataNode Container logs follow:\n" + primaryNode.getLogs());
            throw retryException;
        }
    }

    private ValidatableResponse getStatus(Integer mappedPort) {
        return RestAssured.given()
                .get("http://localhost:" + mappedPort + "/_cluster/health")
                .then();
    }
}
