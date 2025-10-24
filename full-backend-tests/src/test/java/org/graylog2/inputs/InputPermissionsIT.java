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

import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.VM)
public class InputPermissionsIT {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static GraylogApis apis;

    private static GraylogApiResponse roleInputsReader;
    private static GraylogApiResponse roleInputsCreator;
    private static GraylogApiResponse roleRestrictedInputsCreator;

    private static Users.User inputsReader;
    private static Users.User inputsCreator;
    private static Users.User restrictedInputsCreator;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) throws Exception {
        apis = graylogApis;
        roleInputsReader = apis.roles().create("custom_inputs_reader", "inputs reader can only see inputs", Set.of(
                RestPermissions.INPUTS_READ
        ), false);
        inputsReader = createUser("inputs.reader", roleInputsReader);

        roleInputsCreator = apis.roles().create("custom_inputs_creator", "inputs creator can only create inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUTS_CREATE,
                RestPermissions.INPUT_TYPES_CREATE
        ), false);
        inputsCreator = createUser("inputs.creator", roleInputsCreator);

        roleRestrictedInputsCreator = apis.roles().create("custom_restricted_inputs_creator", "inputs creator can only create certain inputs", Set.of(
                RestPermissions.INPUTS_READ,
                RestPermissions.INPUTS_CREATE,
                RestPermissions.INPUT_TYPES_CREATE + ":org.graylog2.inputs.random.FakeHttpMessageInput",
                RestPermissions.INPUT_TYPES_CREATE + ":org.graylog2.inputs.gelf.tcp.GELFTCPInput"

        ), false);
        restrictedInputsCreator = createUser("restricted.inputs.creator", roleRestrictedInputsCreator);

        waitForRolesCacheRefresh();

        apis.users().createUser(inputsReader);
        apis.users().createUser(inputsCreator);
        apis.users().createUser(restrictedInputsCreator);
    }

    /**
     * Roles are stored in mongodb, but the auth backend is refreshing those only once every second. If we trigger a call
     * before the role is refreshed, we may get weird results.
     *
     * @see org.graylog2.security.InMemoryRolePermissionResolver
     */
    private static void waitForRolesCacheRefresh() {
        try {
            // This is naive and wrong, but very simple to implement. Another approach would be to have a fingerprint of
            // roles cache, similarly to StreamRouterEngine and its fingerprint.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Users.User createUser(String username, GraylogApiResponse... roles) {
        return new Users.User(username, RandomString.make(), "<Generated>", username,
                username + "@graylog", false, 30_0000, "Europe/Vienna",
                Arrays.stream(roles).map(role -> role.properJSONPath().read("name", String.class)).toList(), List.of());
    }

    @AfterAll
    void tearDown() {
        apis.users().deleteUser(inputsReader.username());
        apis.users().deleteUser(inputsCreator.username());
        apis.users().deleteUser(restrictedInputsCreator.username());

        apis.roles().delete(roleInputsReader.properJSONPath().read("name", String.class));
        apis.roles().delete(roleInputsCreator.properJSONPath().read("name", String.class));
        apis.roles().delete(roleRestrictedInputsCreator.properJSONPath().read("name", String.class));
    }

    @FullBackendTest
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
                "Timed out waiting for HTTP Random Message Input to become available", TIMEOUT);

        apis.forUser(inputsReader).inputs().getInput(inputId).assertThat().body("id", equalTo(inputId));

        apis.inputs().deleteInput(inputId);

        String inputId2 = apis.forUser(restrictedInputsCreator).inputs().createGlobalInput("testInput",
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                Map.of("sleep", 30,
                        "sleep_deviation", 30,
                        "source", "example.org"));
        apis.waitFor(() ->
                        apis.inputs().getInputState(inputId2)
                                .extract().body().jsonPath().get("state")
                                .equals("RUNNING"),
                "Timed out waiting for HTTP Random Message Input to become available", TIMEOUT);

        apis.forUser(inputsReader).inputs().getInput(inputId2).assertThat().body("id", equalTo(inputId2));

        apis.inputs().deleteInput(inputId2);
    }

    @FullBackendTest
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
                "Timed out waiting for Json Input to become available", TIMEOUT);

        apis.forUser(inputsReader).inputs().getInput(inputId).assertThat().body("id", equalTo(inputId));

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

    @FullBackendTest
    void testInputTypesRead() {
        final GraylogApiResponse inputTypesForReader = apis.forUser(inputsReader).inputs().getInputTypes();
        final Map<String, String> typesReader = inputTypesForReader.properJSONPath().read("types");
        Assertions.assertThat(typesReader).isEmpty();

        final GraylogApiResponse inputTypesForRestrictedCreator = apis.forUser(restrictedInputsCreator).inputs().getInputTypes();
        final Map<String, String> typesRestricted = inputTypesForRestrictedCreator.properJSONPath().read("types");
        Assertions.assertThat(typesRestricted).containsOnlyKeys(
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                "org.graylog2.inputs.gelf.tcp.GELFTCPInput"
        );

        final GraylogApiResponse inputTypesForCreator = apis.forUser(inputsCreator).inputs().getInputTypes();
        final Map<String, String> typesCreator = inputTypesForCreator.properJSONPath().read("types");
        Assertions.assertThat(typesCreator).hasSizeGreaterThan(5);
    }
}
