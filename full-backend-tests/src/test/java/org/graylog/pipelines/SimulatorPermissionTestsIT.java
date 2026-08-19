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

@GraylogBackendConfiguration
public class SimulatorPermissionTestsIT {
    private static final Map<String, Object> TEST_MESSAGE = Map.of(
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

    @FullBackendTest
    void testSimulateNotPermittedForReader() {
        api.forUser(Users.JOHN_DOE).simulator().simulate(DEFAULT_STREAM_ID, TEST_MESSAGE, HttpStatus.SC_FORBIDDEN);
    }

    @FullBackendTest
    void testSimulatePermittedForAdmin() {
        api.forUser(Users.LOCAL_ADMIN).simulator().simulate(DEFAULT_STREAM_ID, TEST_MESSAGE, HttpStatus.SC_OK);
    }

    @FullBackendTest
    void testSimulatePermittedForUser() {
        api.forUser(simulatorUser).simulator().simulate(DEFAULT_STREAM_ID, TEST_MESSAGE, HttpStatus.SC_OK);
    }
}
