/*
 *
 *  * Copyright (C) 2020 Graylog, Inc.
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the Server Side Public License, version 1,
 *  * as published by MongoDB, Inc.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * Server Side Public License for more details.
 *  *
 *  * You should have received a copy of the Server Side Public License
 *  * along with this program. If not, see
 *  * <http://www.mongodb.com/licensing/server-side-public-license>.
 *
 */
package org.graylog2.inputs;

import io.restassured.path.json.JsonPath;
import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.shared.security.RestPermissions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.VM, searchVersions = SearchServer.DATANODE_DEV)
public class InputPermissionsIT {

    private final GraylogApis apis;

    private GraylogApiResponse roleInputsReader;
    private GraylogApiResponse roleRestrictedInputsReader;
    private GraylogApiResponse roleInputsCreator;
    private GraylogApiResponse roleRestrictedInputsCreator;

    private Users.User inputsReader;
    private Users.User restrictedInputsReader;
    private Users.User inputsCreator;
    private Users.User restrictedInputsCreator;

    public InputPermissionsIT(GraylogApis apis) {
        this.apis = apis;
    }

    @BeforeAll
    void setUp() {
        roleInputsReader = apis.roles().create("custom_inputs_reader", "inputs reader can only see inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUT_TYPES_READ
        ), false);
        inputsReader = createUser("inputs.reader", roleInputsReader);

        roleRestrictedInputsReader = apis.roles().create("custom_restricted_inputs_reader", "inputs reader can only see two input types", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUT_TYPES_READ + ":org.graylog2.inputs.random.FakeHttpMessageInput",
                RestPermissions.INPUT_TYPES_READ + ":org.graylog2.inputs.gelf.tcp.GELFTCPInput"
        ), false);
        restrictedInputsReader = createUser("restricted.inputs.reader", roleRestrictedInputsReader);

        roleInputsCreator = apis.roles().create("custom_inputs_creator", "inputs creator can only create inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUT_TYPES_READ,
                RestPermissions.INPUTS_CREATE,
                RestPermissions.INPUT_TYPES_CREATE
        ), false);
        inputsCreator = createUser("inputs.creator", roleInputsCreator);

        roleRestrictedInputsCreator = apis.roles().create("custom_inputs_admin", "inputs admin manages inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUT_TYPES_READ + ":org.graylog2.inputs.random.FakeHttpMessageInput",
                RestPermissions.INPUTS_CREATE,
                RestPermissions.INPUT_TYPES_CREATE + ":org.graylog2.inputs.random.FakeHttpMessageInput"
        ), false);
        restrictedInputsCreator = createUser("restricted.inputs.creator", roleRestrictedInputsCreator);

        JsonPath user = apis.users().createUser(inputsReader);
        user = apis.users().createUser(restrictedInputsReader);
        user = apis.users().createUser(inputsCreator);
        user = apis.users().createUser(restrictedInputsCreator);
    }

    private Users.@NotNull User createUser(String username, GraylogApiResponse... roles) {
        return new Users.User(username, RandomString.make(), "<Generated>", username,
                username + "@graylog", false, 30_0000, "Europe/Vienna",
                Arrays.stream(roles).map(role -> role.properJSONPath().read("name", String.class)).toList(), List.of());
    }

    @AfterAll
    void tearDown() {
        apis.users().deleteUser(inputsReader.username());
        apis.users().deleteUser(restrictedInputsReader.username());
        apis.users().deleteUser(inputsCreator.username());
        apis.users().deleteUser(restrictedInputsCreator.username());

        apis.roles().delete(roleInputsReader.properJSONPath().read("name", String.class));
        apis.roles().delete(roleRestrictedInputsReader.properJSONPath().read("name", String.class));
        apis.roles().delete(roleInputsCreator.properJSONPath().read("name", String.class));
        apis.roles().delete(roleRestrictedInputsCreator.properJSONPath().read("name", String.class));
    }

    @ContainerMatrixTest
    void testPermittedInputCreationAndReading() {
        String inputId = apis.forUser(inputsCreator).inputs().createGlobalInput("testInput",
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                Map.of("sleep", 30,
                        "sleep_deviation", 30,
                        "source", "example.org"));
        apis.waitFor(() ->
                        apis.inputs().getInputState(inputId)
                                .extract().body().jsonPath().get("state")
                                .equals("RUNNING"),
                "Timed out waiting for HTTP Random Message Input to become available");

        apis.forUser(inputsReader).inputs().getInput(inputId).assertThat().body("id", equalTo(inputId));
        apis.forUser(restrictedInputsReader).inputs().getInput(inputId).assertThat().body("id", equalTo(inputId));

        apis.inputs().deleteInput(inputId);
    }

    @ContainerMatrixTest
    void testRestrictedInputCreationAndReading() {
        String inputId = apis.forUser(inputsCreator).inputs().createGlobalInput("testInput",
                "org.graylog2.inputs.misc.jsonpath.JsonPathInput",
                Map.of("target_url", "https://example.org",
                        "interval", 10,
                        "timeunit", "MINUTES",
                        "path", "$.data",
                        "source", "messagesource"));
        apis.waitFor(() ->
                        apis.inputs().getInputState(inputId)
                                .extract().body().jsonPath().get("state")
                                .equals("RUNNING"),
                "Timed out waiting for Json Input to become available");

        apis.forUser(inputsReader).inputs().getInput(inputId).assertThat().body("id", equalTo(inputId));
        Assertions.assertThatThrownBy(() -> apis.forUser(restrictedInputsCreator).inputs().getInput(inputId))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected status code <200> but was <403>");

        apis.inputs().deleteInput(inputId);

        Assertions.assertThatThrownBy(() -> apis.forUser(restrictedInputsCreator).inputs().createGlobalInput("testInput",
                        "org.graylog2.inputs.misc.jsonpath.JsonPathInput",
                        Map.of("target_url", "https://example.org",
                                "interval", 10,
                                "timeunit", "MINUTES",
                                "path", "$.data",
                                "source", "messagesource")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected status code <201> but was <403>");

    }

    @ContainerMatrixTest
    void testInputTypesRead() {
        final GraylogApiResponse inputTypesForReader = apis.forUser(inputsReader).inputs().getInputTypes();
        final Map<String, String> typesReader = inputTypesForReader.properJSONPath().read("types");
        Assertions.assertThat(typesReader).hasSizeGreaterThan(5);

        final GraylogApiResponse inputTypesForRestrictedReader = apis.forUser(restrictedInputsReader).inputs().getInputTypes();
        final Map<String, String> typesRestricted = inputTypesForRestrictedReader.properJSONPath().read("types");
        Assertions.assertThat(typesRestricted).containsOnlyKeys(
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                "org.graylog2.inputs.gelf.tcp.GELFTCPInput"
        );
    }
}
