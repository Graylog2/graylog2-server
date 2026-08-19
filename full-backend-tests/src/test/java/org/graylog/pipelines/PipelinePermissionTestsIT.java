package org.graylog.pipelines;

import net.bytebuddy.utility.RandomString;
import org.apache.http.HttpStatus;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Set;


@GraylogBackendConfiguration
public class PipelinePermissionTestsIT {
    private static GraylogApis api;

    private static Users.User ruleCreator;
    private static GraylogApiResponse ruleCreatorRole;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) {
        api = graylogApis;
        ruleCreatorRole = api.roles().create("custom_rule_creator", "test role for allowing rule creation", Set.of(
                PipelineRestPermissions.PIPELINE_RULE_CREATE
        ), false);
        ruleCreator = api.users().generateUserWithDefaults("inputs.reader", RandomString.make(), ruleCreatorRole);
        api.users().createUser(ruleCreator);
    }

    @AfterAll
    void tearDown() {
        api.users().deleteUser(ruleCreator.username());
        api.roles().delete(ruleCreatorRole.properJSONPath().read("name", String.class));
    }

    @FullBackendTest
    void testParseNotPermittedForReader() {
        api.forUser(Users.JOHN_DOE).pipelines().parse("Title", "Description", "Source", HttpStatus.SC_FORBIDDEN);
    }

    @FullBackendTest
    void testParsePermittedForAdmin() {
        api.forUser(Users.LOCAL_ADMIN).pipelines().parse("Title", "Description", "Source", HttpStatus.SC_OK);
    }
    @FullBackendTest
    void testParsePermittedForUser() {
        api.forUser(ruleCreator).pipelines().parse("Title", "Description", "Source", HttpStatus.SC_OK);
    }
}
