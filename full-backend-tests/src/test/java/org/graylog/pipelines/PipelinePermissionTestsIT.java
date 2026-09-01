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
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Set;


@GraylogBackendConfiguration
public class PipelinePermissionTestsIT {
    private static GraylogApis api;

    private static Users.User ruleCreator;
    private static GraylogApiResponse ruleCreatorRole;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) {
        api = graylogApis;
        ruleCreatorRole = api.roles().create("custom_pipeline_creator", "test role for allowing pipeline creation", Set.of(
                PipelineRestPermissions.PIPELINE_CREATE
        ), false);
        ruleCreator = api.users().generateUserWithDefaults("pipeline.creator", RandomString.make(), ruleCreatorRole);
        api.users().createUser(ruleCreator);
    }

    private String createPipelineSource(String ruleName) {
        return """
                pipeline "MyPipe"
                stage 0 match either
                rule "%s"
                end
                """.formatted(ruleName);
    }

    @AfterAll
    void tearDown() {
        api.users().deleteUser(ruleCreator.username());
        api.roles().delete(ruleCreatorRole.properJSONPath().read("name", String.class));
    }

    @FullBackendTest
    void testParseNotPermittedForReader() {
        api.forUser(Users.JOHN_DOE).pipelines().parse("Title", "Description", "pipeline \"test1\"\nstage 0 match either\nend", HttpStatus.SC_UNAUTHORIZED);
    }

    @FullBackendTest
    void testParsePermittedForAdmin() {
        api.forUser(Users.LOCAL_ADMIN).pipelines().parse("Title", "Description", "pipeline \"test2\"\nstage 0 match either\nend", HttpStatus.SC_OK);
    }
    @FullBackendTest
    void testParsePermittedForUser() {
        api.forUser(ruleCreator).pipelines().parse("Title", "Description", "pipeline \"test3\"\nstage 0 match either\nend", HttpStatus.SC_OK);
    }
}
