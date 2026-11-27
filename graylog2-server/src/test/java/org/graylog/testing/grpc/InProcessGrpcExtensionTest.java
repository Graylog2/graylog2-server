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
package org.graylog.testing.grpc;

import io.grpc.ManagedChannel;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(InProcessGrpcExtension.class)
class InProcessGrpcExtensionTest {
    private static final List<ManagedChannel> CHANNELS = new ArrayList<>();

    private InProcessGrpc beforeEachGrpc;

    @BeforeEach
    void setUp(InProcessGrpc grpc) {
        beforeEachGrpc = grpc;
    }

    @Test
    void injectsSameInstanceIntoLifecycleAndTestMethods(InProcessGrpc grpc) {
        assertThat(grpc).isSameAs(beforeEachGrpc);
    }

    @Test
    void startsServiceAndConnectsChannel(InProcessGrpc grpc) throws IOException {
        final ManagedChannel channel = grpc.start(new HealthStatusManager().getHealthService());
        CHANNELS.add(channel);

        assertThat(HealthGrpc.newBlockingStub(channel).check(HealthCheckRequest.getDefaultInstance()).getStatus())
                .isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
    }

    @AfterAll
    static void shutsDownChannelsAfterEachTest() {
        // CHANNELS is empty if startsServiceAndConnectsChannel() didn't run, e.g. when running a single test method.
        assertThat(CHANNELS).allSatisfy(channel -> assertThat(channel.isTerminated()).isTrue());
    }
}
