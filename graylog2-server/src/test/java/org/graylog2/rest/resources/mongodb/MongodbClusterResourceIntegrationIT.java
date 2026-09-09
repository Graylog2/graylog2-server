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
package org.graylog2.rest.resources.mongodb;

import com.mongodb.MongoClient;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog2.cluster.nodes.mongodb.MongodbClusterCommand;
import org.graylog2.cluster.nodes.mongodb.MongodbConnectionResolver;
import org.graylog2.cluster.nodes.mongodb.MongodbNode;
import org.graylog2.cluster.nodes.mongodb.MongodbNodesProvider;
import org.graylog2.cluster.nodes.mongodb.MongodbNodesService;
import org.graylog2.cluster.nodes.mongodb.ProfilingLevel;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for MongoDB permission handling using testcontainers.
 *
 * This test creates a real MongoDB instance with authentication enabled and tests
 * the error handling when a user lacks the required permissions.
 */
@Testcontainers
class MongodbClusterResourceIntegrationIT {

    private record MongoRole(String name, String db) {}

    // username/password for a MongoDB user, the database it was created on (also used as the auth source),
    // and the roles it was granted (empty for the root user, which is created by the container itself).
    private record TestUser(String username, String password, String db, List<MongoRole> roles) {}

    private static final String TEST_DATABASE = "graylog";
    private static final String ADMIN_DATABASE = "admin";

    // MongoDB 8.0.29 introduced SERVER-130198, which raises the authorization requirements for the
    // profile command when it includes fields like "slowms". Pin to a version that has this change
    // so a regression there is actually caught, instead of relying on MongoDBVersion.DEFAULT (7.0).
    private static final MongoDBVersion PROFILING_TEST_MONGODB_VERSION = MongoDBVersion.of("8.0.29");

    // Root superuser, provisioned by the container itself via MONGO_INITDB_ROOT_* env vars below.
    private static final TestUser ADMIN = new TestUser("admin", "adminpass", ADMIN_DATABASE, List.of());

    // Read/write access to data, but no profiling-related privileges at all.
    private static final TestUser RESTRICTED = new TestUser("restricteduser", "restrictedpass", TEST_DATABASE,
            List.of(new MongoRole("readWrite", TEST_DATABASE)));

    // Read-only cluster monitoring role: grants the 'profiler' action (read status) but not
    // 'enableProfiler' (change it).
    private static final TestUser CLUSTER_MONITOR = new TestUser("clustermonitoruser", "clustermonitorpass", ADMIN_DATABASE,
            List.of(new MongoRole("clusterMonitor", ADMIN_DATABASE)));

    // The full set of permissions documented for the Graylog MongoDB user.
    private static final TestUser DB_ADMIN = new TestUser("dbadminuser", "dbadminpass", TEST_DATABASE,
            List.of(
                    new MongoRole("dbAdmin", TEST_DATABASE),
                    new MongoRole("readWrite", TEST_DATABASE),
                    new MongoRole("clusterMonitor", ADMIN_DATABASE)
            ));

    // Only dbAdmin, nothing else — exactly what our 403 hints tell customers to grant.
    private static final TestUser DB_ADMIN_ONLY = new TestUser("dbadminonlyuser", "dbadminonlypass", TEST_DATABASE,
            List.of(new MongoRole("dbAdmin", TEST_DATABASE)));

    private static final MongodbNodesService NO_OP_NODES_SERVICE = new MongodbNodesService() {
        @Override
        public List<MongodbNode> allNodes() {
            return List.of();
        }

        @Override
        public boolean available() {
            return true;
        }
    };

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:" + PROFILING_TEST_MONGODB_VERSION.version())
            .withEnv("MONGO_INITDB_ROOT_USERNAME", ADMIN.username())
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", ADMIN.password())
            .withEnv("MONGO_INITDB_DATABASE", TEST_DATABASE)
            // MongoDB does a 2-phase start when auth is enabled: starts without auth, creates the admin user,
            // restarts with --auth. Wait for the 2nd "Waiting for connections" to ensure auth is fully set up.
            .waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 2));

    private static MongoClient adminClient;

    @BeforeAll
    static void setup() {
        adminClient = new MongoClient(buildConnectionUri(ADMIN));
        List.of(RESTRICTED, CLUSTER_MONITOR, DB_ADMIN, DB_ADMIN_ONLY).forEach(MongodbClusterResourceIntegrationIT::createMongoUser);
    }

    @AfterAll
    static void teardown() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    @Test
    void changeProfiling_succeeds_forRootUser() {
        Response response = createResource(ADMIN).changeProfiling(ProfilingLevel.SLOW_OPS);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void profilingStatus_succeeds_forRootUser() {
        Response response = createResource(ADMIN).profilingStatus();

        assertProfilingStatusOk(response);
    }

    @Test
    void changeProfiling_returns403_forLevelALL_evenForRootUser() {
        // Setting profiling to ALL is rejected regardless of permissions - it's not a permission problem.
        Response response = createResource(ADMIN).changeProfiling(ProfilingLevel.ALL);

        assertThat(response.getStatus()).isEqualTo(403);

        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertThat(entity.get("error")).isEqualTo("Invalid profiling level provided");
        assertThat(entity.get("message")).contains("ALL");
        assertThat(entity.get("hint")).contains("performance");
    }

    @Test
    void changeProfiling_returns403_forRestrictedUser() {
        Response response = createResource(RESTRICTED).changeProfiling(ProfilingLevel.SLOW_OPS);

        assertPermissionDenied(response, "enableProfiler");
    }

    @Test
    void profilingStatus_returns403_forRestrictedUser() {
        Response response = createResource(RESTRICTED).profilingStatus();

        assertPermissionDenied(response, "clusterMonitor");
    }

    @Test
    void profilingStatus_succeeds_forClusterMonitorUser() {
        Response response = createResource(CLUSTER_MONITOR).profilingStatus();

        assertProfilingStatusOk(response);
    }

    @Test
    void changeProfiling_returns403_forClusterMonitorUser() {
        // clusterMonitor grants read-only monitoring (incl. reading profiling status) but not enableProfiler.
        Response response = createResource(CLUSTER_MONITOR).changeProfiling(ProfilingLevel.SLOW_OPS);

        assertPermissionDenied(response, "enableProfiler");
    }

    @Test
    void changeProfiling_succeeds_forDbAdminUser() {
        // Mirrors the documented Graylog MongoDB user: dbAdmin + readWrite on graylog, clusterMonitor on
        // admin - no root. This is the setup that regressed under MongoDB 8.0.29 (SERVER-130198) when the
        // profile command included "slowms".
        Response response = createResource(DB_ADMIN).changeProfiling(ProfilingLevel.SLOW_OPS);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void changeProfiling_succeeds_forDbAdminOnlyUser() {
        // Verifies that dbAdmin alone - our 403 hint's suggested remediation - is actually sufficient.
        Response response = createResource(DB_ADMIN_ONLY).changeProfiling(ProfilingLevel.SLOW_OPS);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void profilingStatus_succeeds_forDbAdminOnlyUser() {
        // dbAdmin grants the 'profiler' action too, so status should be readable without clusterMonitor.
        Response response = createResource(DB_ADMIN_ONLY).profilingStatus();

        assertProfilingStatusOk(response);
    }

    private static void assertPermissionDenied(Response response, String hintFragment) {
        assertThat(response.getStatus()).isEqualTo(403);

        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertThat(entity.get("error")).isEqualTo("Permission denied");
        assertThat(entity).containsKey("message");
        assertThat(entity.get("hint")).contains(hintFragment);
    }

    private static void assertProfilingStatusOk(Response response) {
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isNotNull();
    }

    private static void createMongoUser(TestUser user) {
        List<Document> roleDocuments = user.roles().stream()
                .map(role -> new Document("role", role.name()).append("db", role.db()))
                .toList();

        adminClient.getDatabase(user.db()).runCommand(new Document()
                .append("createUser", user.username())
                .append("pwd", user.password())
                .append("roles", roleDocuments));
    }

    private static MongodbClusterResource createResource(TestUser user) {
        MongoConnection mongoConnection = createMongoConnection(user);
        MongodbNodesProvider nodesProvider = new MongodbNodesProvider(mongoConnection, Set.of(NO_OP_NODES_SERVICE));

        // Create a new client for each call (matching production behavior)
        MongodbConnectionResolver connectionResolver = host -> new MongoClient(buildConnectionUri(user));

        MongodbClusterCommand clusterCommand = new MongodbClusterCommand(mongoConnection, connectionResolver);

        return new MongodbClusterResource(nodesProvider, clusterCommand);
    }

    private static MongoConnection createMongoConnection(TestUser user) {
        MongoDbConfiguration config = new MongoDbConfiguration();
        config.setUri(buildConnectionUri(user));
        return new MongoConnectionImpl(config);
    }

    private static String buildConnectionUri(TestUser user) {
        return String.format(Locale.ROOT,
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                user.username(),
                user.password(),
                mongoContainer.getHost(),
                mongoContainer.getFirstMappedPort(),
                TEST_DATABASE,
                user.db()
        );
    }
}
