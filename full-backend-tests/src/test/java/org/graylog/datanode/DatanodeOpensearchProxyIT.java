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
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV, additionalConfigurationParameters = {@ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "true")})
public class DatanodeOpensearchProxyIT {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeOpensearchProxyIT.class);
    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    void testProxyPlaintextGet() throws ExecutionException, RetryException {
        waitForLeader();
        final ValidatableResponse response = apis.get("/datanodes/leader/request/_cat/indices", 200);
        final String responseBody = response.extract().body().asString();
        Assertions.assertThat(responseBody).contains(".ds-gl-datanode-metrics").contains("graylog_0").contains("gl-system-events_0");
    }

    @ContainerMatrixTest
    void testProxyJsonGet() {
        final ValidatableResponse response = apis.get("/datanodes/any/request/_mapping", 200);
        response.assertThat().body("graylog_0.mappings.properties.gl2_accounted_message_size.type", Matchers.equalTo("long"));
    }

    @ContainerMatrixTest
    void testForbiddenUrl() {
        final String message = apis.get("/datanodes/any/request/_search", 400).extract().body().asString();
        Assertions.assertThat(message).contains("This request is not allowed");
    }

    @ContainerMatrixTest
    void testTargetSpecificDatanodeInstance() {
        final List<String> datanodes = getDatanodes().properJSONPath().read("elements.*.hostname");
        Assertions.assertThat(datanodes).isNotEmpty();

        final String hostname = datanodes.iterator().next();
        apis.get("/datanodes/" + hostname + "/request/_mapping", 200).assertThat().body("graylog_0.mappings.properties.gl2_accounted_message_size.type", Matchers.equalTo("long"));
    }

    private GraylogApiResponse getDatanodes() {
        return new GraylogApiResponse(apis.get("/system/cluster/datanodes", 200));
    }

    /**
     * A datanode becomes leader if the underlying opensearch node becomes leader. This may take a while during the
     * startup. Our periodical checks the leader status every 10s, so in this interval there should appear one leader
     * for sure.
     *
     * @see org.graylog.datanode.periodicals.ClusterManagerDiscovery
     */
    private void waitForLeader() throws ExecutionException, RetryException {
        RetryerBuilder.<GraylogApiResponse>newBuilder()
                .retryIfResult(r -> !isAnyLeader(r))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        LOG.info("Waiting for one datanode to become leader, attempt {}", attempt.getAttemptNumber());
                    }
                })
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(10))
                .build()
                .call(this::getDatanodes);
    }

    private boolean isAnyLeader(GraylogApiResponse r) {
        List<Boolean> isLeader = r.properJSONPath().read("elements.*.is_leader");
        return isLeader.stream().anyMatch(leader -> leader);
    }
}
