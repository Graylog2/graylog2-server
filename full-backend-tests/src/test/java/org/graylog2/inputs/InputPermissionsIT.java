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
package org.graylog2.inputs;

import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV)
public class InputPermissionsIT {

    private final GraylogApis apis;

    public InputPermissionsIT(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    void testHttpRandomInputCreation() {


        final GraylogApiResponse roleInputsAdmin = apis.roles().createRole("custom_inputs_admin", "inputs admin manages inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUTS_CREATE,
                RestPermissions.INPUTS_CHANGESTATE,
                RestPermissions.INPUTS_EDIT,
                RestPermissions.INPUTS_TERMINATE
        ), false);

        final GraylogApiResponse roleInputsReader = apis.roles().createRole("custom_inputs_reader", "inputs reader can only see inputs", Set.of(
                RestPermissions.INPUTS_READ
        ), false);

        final Users.User inputsAdmin = new Users.User("max.admin", "asdfgh", "Max", "Admin", "max.admin@graylog", false, 30_0000, "Europe/Vienna", List.of(roleInputsAdmin.properJSONPath().read("name", String.class)), List.of());
        final Users.User inputsReader = new Users.User("inputs_reader", "qwertz", "Joe", "Reader", "joe.reader@graylog", false, 30_0000, "Europe/Vienna", List.of(roleInputsReader.properJSONPath().read("name", String.class)), List.of());

        apis.users().createUser(inputsAdmin);
        apis.users().createUser(inputsReader);

        String inputId = apis.forUser(inputsAdmin).inputs().createGlobalInput("testInput",
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                Map.of("sleep", 30,
                        "sleep_deviation", 30,
                        "source", "example.org"));

        apis.inputs().getInput(inputId).assertThat().body("title", equalTo("testInput"));
        apis.waitFor(() ->
                        apis.inputs().getInputState(inputId)
                                .extract().body().jsonPath().get("state")
                                .equals("RUNNING"),
                "Timed out waiting for HTTP Random Message Input to become available");
        apis.inputs().deleteInput(inputId);

        Assertions.assertThatThrownBy(() -> apis.forUser(inputsReader).inputs().createGlobalInput("testInput",
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                Map.of("sleep", 30,
                        "sleep_deviation", 30,
                        "source", "example.org")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected status code <201> but was <403>");

    }
}
