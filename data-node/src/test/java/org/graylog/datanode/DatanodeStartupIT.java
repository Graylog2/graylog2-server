package org.graylog.datanode;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.http.NoHttpResponseException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.net.SocketException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DatanodeStartupIT {

    private DockerComposeContainer container;

    @BeforeEach
    void setUp() {
        final Path dockerfilePath = Path.of("docker-compose.yml");
        container = new DockerComposeContainer(dockerfilePath.toFile());
        container.start();
    }

    @AfterEach
    void tearDown() {
        container.stop();
    }

    @Test
    void testDatanodeStartup() throws ExecutionException, RetryException {

        final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(60))
                .retryIfException(input -> input instanceof NoHttpResponseException)
                .retryIfException(input -> input instanceof SocketException)
                .retryIfResult(input -> !input.extract().body().path("process.info.status").equals("AVAILABLE"))
                .build();

        retryer.call(this::getStatus)
                .assertThat()
                .body("process.info.node_name", Matchers.equalTo("node1"))
                .body("process.info.pid", Matchers.notNullValue())
                .body("process.info.user", Matchers.equalTo("opensearch"));
    }

    private ValidatableResponse getStatus() {
        return RestAssured.given()
                .get("http://localhost:8999")
                .then();
    }
}
