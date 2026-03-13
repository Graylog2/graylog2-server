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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Collections;
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

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "adminpass";
    private static final String RESTRICTED_USER = "restricteduser";
    private static final String RESTRICTED_PASSWORD = "restrictedpass";
    private static final String TEST_DATABASE = "graylog";

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:" + MongoDBVersion.DEFAULT.version())
            .withEnv("MONGO_INITDB_ROOT_USERNAME", ADMIN_USER)
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", ADMIN_PASSWORD)
            .withEnv("MONGO_INITDB_DATABASE", TEST_DATABASE);

    private static MongoClient adminClient;
    private static MongoClient restrictedClient;

    @BeforeAll
    static void setup() {
        // Create admin client
        String host = mongoContainer.getHost();
        int port = mongoContainer.getFirstMappedPort();

        String adminUri = String.format(Locale.ROOT,
                "mongodb://%s:%s@%s:%d/admin",
                ADMIN_USER,
                ADMIN_PASSWORD,
                host,
                port
        );

        adminClient = new MongoClient(adminUri);

        // Create a restricted user with read/write but NO profiling permissions
        adminClient.getDatabase(TEST_DATABASE).runCommand(new Document()
                .append("createUser", RESTRICTED_USER)
                .append("pwd", RESTRICTED_PASSWORD)
                .append("roles", Collections.singletonList(
                        new Document("role", "readWrite").append("db", TEST_DATABASE)
                ))
        );

        // Create restricted client
        String restrictedUri = String.format(Locale.ROOT,
                "mongodb://%s:%s@%s:%d/%s",
                RESTRICTED_USER,
                RESTRICTED_PASSWORD,
                host,
                port,
                TEST_DATABASE
        );

        restrictedClient = new MongoClient(restrictedUri);
    }

    @AfterAll
    static void teardown() {
        if (adminClient != null) {
            adminClient.close();
        }
        if (restrictedClient != null) {
            restrictedClient.close();
        }
    }

    @Test
    void enableProfiling_returns403_whenUserLacksPermissions() {
        // Given: Create resource with restricted MongoDB connection
        MongodbClusterResource resource = createResourceWithRestrictedUser();

        // When: Try to enable profiling (should fail due to lack of permissions)
        Response response = resource.enableProfiling(ProfilingLevel.ALL);

        // Then: Should return 403 Forbidden
        assertThat(response.getStatus()).isEqualTo(403);

        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertThat(entity).containsKey("error");
        assertThat(entity).containsKey("message");
        assertThat(entity).containsKey("hint");
        assertThat(entity.get("error")).isEqualTo("Permission denied");
        assertThat(entity.get("hint")).contains("enableProfiler");
    }

    @Test
    void profilingStatus_returns403_whenUserLacksPermissions() {
        // Given: Create resource with restricted MongoDB connection
        MongodbClusterResource resource = createResourceWithRestrictedUser();

        // When: Try to get profiling status (should fail due to lack of permissions)
        Response response = resource.profilingStatus();

        // Then: Should return 403 Forbidden
        assertThat(response.getStatus()).isEqualTo(403);

        @SuppressWarnings("unchecked")
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertThat(entity).containsKey("error");
        assertThat(entity).containsKey("message");
        assertThat(entity).containsKey("hint");
        assertThat(entity.get("error")).isEqualTo("Permission denied");
        assertThat(entity.get("hint")).contains("clusterMonitor");
    }

    @Test
    void enableProfiling_succeeds_whenUserHasPermissions() {
        // Given: Create resource with admin MongoDB connection
        MongodbClusterResource resource = createResourceWithAdminUser();

        // When: Try to enable profiling with proper permissions
        Response response = resource.enableProfiling(ProfilingLevel.ALL);

        // Then: Should succeed
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void profilingStatus_succeeds_whenUserHasPermissions() {
        // Given: Create resource with admin MongoDB connection
        MongodbClusterResource resource = createResourceWithAdminUser();

        // When: Try to get profiling status with proper permissions
        Response response = resource.profilingStatus();

        // Then: Should succeed
        assertThat(response.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<ProfilingLevel, Long> entity = (Map<ProfilingLevel, Long>) response.getEntity();
        assertThat(entity).isNotNull();
    }

    private MongodbClusterResource createResourceWithRestrictedUser() {
        MongoConnection mongoConnection = createMongoConnection(RESTRICTED_USER, RESTRICTED_PASSWORD);
        return createResource(mongoConnection);
    }

    private MongodbClusterResource createResourceWithAdminUser() {
        MongoConnection mongoConnection = createMongoConnection(ADMIN_USER, ADMIN_PASSWORD, "admin");
        return createResource(mongoConnection);
    }

    private MongodbClusterResource createResource(MongoConnection mongoConnection) {
        MongodbNodesProvider nodesProvider = new MongodbNodesProvider(Set.of(new MongodbNodesService() {
            @Override
            public List<MongodbNode> allNodes() {
                return List.of();
            }

            @Override
            public boolean available() {
                return true;
            }
        }));

        MongodbConnectionResolver connectionResolver = host -> {
            // For standalone setup, return the same client
            return restrictedClient;
        };

        MongodbClusterCommand clusterCommand = new MongodbClusterCommand(
                mongoConnection,
                connectionResolver
        );

        return new MongodbClusterResource(nodesProvider, clusterCommand);
    }

    private MongoConnection createMongoConnection(String username, String password) {
        return createMongoConnection(username, password, TEST_DATABASE);
    }

    private MongoConnection createMongoConnection(String username, String password, String authDatabase) {
        String host = mongoContainer.getHost();
        int port = mongoContainer.getFirstMappedPort();

        MongoDbConfiguration config = new MongoDbConfiguration();
        config.setUri(String.format(Locale.ROOT,
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                username,
                password,
                host,
                port,
                TEST_DATABASE,
                authDatabase
        ));

        return new MongoConnectionImpl(config);
    }
}
