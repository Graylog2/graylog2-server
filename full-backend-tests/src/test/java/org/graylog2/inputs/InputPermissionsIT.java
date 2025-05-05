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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.VM, searchVersions = SearchServer.DATANODE_DEV, additionalConfigurationParameters = {
        @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_RESTRICT_INPUT_TYPES", value = "true")
})
public class InputPermissionsIT {

    private final GraylogApis apis;

    private GraylogApiResponse roleInputsAdmin;
    private GraylogApiResponse roleInputsReader;
    private GraylogApiResponse roleInputTypesReader;

    private Users.User inputsAdmin;
    private Users.User inputsReader;
    private Users.User inputTypesReader;

    public InputPermissionsIT(GraylogApis apis) {
        this.apis = apis;
    }

    @BeforeEach
    void setUp() {
        roleInputsAdmin = apis.roles().create("custom_inputs_admin", "inputs admin manages inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUTS_CREATE,
                RestPermissions.INPUTS_CHANGESTATE,
                RestPermissions.INPUTS_EDIT,
                RestPermissions.INPUTS_TERMINATE
        ), false);

        roleInputsReader = apis.roles().create("custom_inputs_reader", "inputs reader can only see inputs", Set.of(
                RestPermissions.INPUTS_READ
        ), false);

        roleInputTypesReader = apis.roles().create("custom_input_types_reader", "inputs types reader can only see input types", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUT_TYPES_READ
        ), false);

        inputsAdmin = new Users.User("max.admin", "asdfgh", "Max", "Admin", "max.admin@graylog", false, 30_0000, "Europe/Vienna", List.of(roleInputsAdmin.properJSONPath().read("name", String.class)), List.of());
        inputsReader = new Users.User("joe.reader", "qwertz", "Joe", "Reader", "joe.reader@graylog", false, 30_0000, "Europe/Vienna", List.of(roleInputsReader.properJSONPath().read("name", String.class)), List.of());
        inputTypesReader = new Users.User("joe.types.reader", "qwertz.types", "Joe", "Types-Reader", "joe.types.reader@graylog", false, 30_0000, "Europe/Vienna", List.of(roleInputTypesReader.properJSONPath().read("name", String.class)), List.of());

        apis.users().createUser(inputsAdmin);
        apis.users().createUser(inputsReader);
        apis.users().createUser(inputTypesReader);
    }

    @AfterEach
    void tearDown() {
        apis.users().deleteUser(inputsAdmin.username());
        apis.users().deleteUser(inputsReader.username());
        apis.users().deleteUser(inputTypesReader.username());

        apis.roles().delete(roleInputsAdmin.properJSONPath().read("name", String.class));
        apis.roles().delete(roleInputsReader.properJSONPath().read("name", String.class));
        apis.roles().delete(roleInputTypesReader.properJSONPath().read("name", String.class));
    }

    @ContainerMatrixTest
    void testHttpRandomInputCreation() {

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

    @ContainerMatrixTest
    void testInputTypesRead() {
        final GraylogApiResponse inputTypesForReader = apis.forUser(inputsReader).inputs().getInputTypes();
        final Map<String, String> typesReader = inputTypesForReader.properJSONPath().read("types");
        Assertions.assertThat(typesReader).isEmpty();

        final GraylogApiResponse inputTypesForTypeReader = apis.forUser(inputTypesReader).inputs().getInputTypes();
        final Map<String, String> types = inputTypesForTypeReader.properJSONPath().read("types");
        Assertions.assertThat(types).isNotEmpty();
    }
}
