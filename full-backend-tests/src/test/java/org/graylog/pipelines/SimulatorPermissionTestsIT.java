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

import net.bytebuddy.utility.RandomString;
import org.apache.http.HttpStatus;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;
import java.util.Set;

import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;

@ContainerMatrixTestsConfiguration
public class SimulatorPermissionTestsIT {
    private static final Map<String, Object> TEST_MESSAGE = Map.of(
                "_id", "1",
            "message", "test message",
            "source", "test"
    );

    private static GraylogApis api;

    private static Users.User simulatorUser;
    private static GraylogApiResponse simulatorRole;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) {
        api = graylogApis;
        simulatorRole = api.roles().create("custom_simulator", "test role for allowing pipeline simulation", Set.of(
                PipelineRestPermissions.PIPELINE_RULE_CREATE,
                RestPermissions.STREAMS_READ
        ), false);
        simulatorUser = api.users().generateUserWithDefaults("simulator.user", RandomString.make(), simulatorRole);
        api.users().createUser(simulatorUser);
    }

    @AfterAll
    void tearDown() {
        api.users().deleteUser(simulatorUser.username());
        api.roles().delete(simulatorRole.properJSONPath().read("name", String.class));
    }

    @ContainerMatrixTest
    void testSimulateNotPermittedForReader() {
        api.forUser(Users.JOHN_DOE).simulator().simulate(DEFAULT_STREAM_ID, TEST_MESSAGE, HttpStatus.SC_UNAUTHORIZED);
    }

    @ContainerMatrixTest
    void testSimulatePermittedForAdmin() {
        api.forUser(Users.LOCAL_ADMIN).simulator().simulate(DEFAULT_STREAM_ID, TEST_MESSAGE, HttpStatus.SC_OK);
    }

    @ContainerMatrixTest
    void testSimulatePermittedForUser() {
        api.forUser(simulatorUser).simulator().simulate(DEFAULT_STREAM_ID, TEST_MESSAGE, HttpStatus.SC_OK);
    }
}
