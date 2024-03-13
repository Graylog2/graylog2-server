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
package org.graylog.testing.completebackend.apis;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DatanodeProxy {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeProxy.class);
    private final GraylogApis graylogApis;

    public DatanodeProxy(GraylogApis graylogApis) {
        this.graylogApis = graylogApis;
    }

    public GraylogApiResponse getDatanodes() {
        return new GraylogApiResponse(graylogApis.get("/system/cluster/datanodes", 200));
    }

    /**
     * A datanode becomes leader if the underlying opensearch node becomes leader. This may take a while during the
     * startup. Our periodical checks the leader status every 10s, so in this interval there should appear one leader
     * for sure.
     */
    public void waitForLeader() throws ExecutionException, RetryException {
        RetryerBuilder.<GraylogApiResponse>newBuilder()
                .retryIfResult(r -> !isAnyLeader(r))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        LOG.info("Waiting for one datanode to become leader, attempt {}", attempt.getAttemptNumber());
                    }
                })
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(60))
                .build()
                .call(this::getDatanodes);
    }

    private boolean isAnyLeader(GraylogApiResponse r) {
        List<Boolean> isLeader = r.properJSONPath().read("elements.*.is_leader");
        return isLeader.stream().anyMatch(leader -> leader);
    }
}
