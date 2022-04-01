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
package org.graylog2.inputs.internal;

import com.google.common.collect.ImmutableMap;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.backenddriver.SearchDriver;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.graylog2.inputs.transports.InternalMetricsTransport;
import org.graylog2.plugin.IOState;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;

import java.util.Arrays;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.OS1, mongoVersions = MongodbServer.MONGO4)
public class InternalMetricsInputIT {

    private final RequestSpecification requestSpec;

    public InternalMetricsInputIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @ContainerMatrixTest
    void testStartInput() {
        GelfInputUtils.createInput(requestSpec, InternalMetricsInput.class, ImmutableMap.of(InternalMetricsTransport.CK_SLEEP, 250), "Integration test internal metrics input");

        final InputStatesList inputStates = GelfInputUtils.getInputStates(requestSpec);
        final boolean inputCreated = inputStates.states().stream()
                .filter(this::isValidInputState)
                .map(InputStateSummary::messageInput)
                .anyMatch(i -> i.type().equals(InternalMetricsInput.class.getName()));

        assertThat(inputCreated).isTrue();

        // wait for a first message
        final boolean isMessagePresent = SearchUtils.waitForMessage(requestSpec, m -> ((String)m.message().get("message")).startsWith("Graylog internal metrics"));
        assertThat(isMessagePresent).isTrue();
    }

    private boolean isValidInputState(InputStateSummary i) {
        return Stream.of(IOState.Type.CREATED, IOState.Type.STARTING, IOState.Type.INITIALIZED, IOState.Type.RUNNING).anyMatch(s -> s == IOState.Type.valueOf(i.state()));
    }
}
