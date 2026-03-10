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
package org.graylog2.cluster.nodes.mongodb;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.Nonnull;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog2.database.MongoConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for MongodbClusterCommand exception handling using real MongoDB.
 */
@Testcontainers
class MongodbClusterCommandIT {

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
        String host = mongoContainer.getHost();
        int port = mongoContainer.getFirstMappedPort();

        String adminUri = String.format("mongodb://%s:%s@%s:%d/admin", ADMIN_USER, ADMIN_PASSWORD, host, port);
        adminClient = new MongoClient(adminUri);

        // Create a restricted user with read/write but NO profiling permissions
        adminClient.getDatabase(TEST_DATABASE).runCommand(new Document()
                .append("createUser", RESTRICTED_USER)
                .append("pwd", RESTRICTED_PASSWORD)
                .append("roles", Collections.singletonList(
                        new Document("role", "readWrite").append("db", TEST_DATABASE)
                ))
        );

        String restrictedUri = String.format("mongodb://%s:%s@%s:%d/%s",
                RESTRICTED_USER, RESTRICTED_PASSWORD, host, port, TEST_DATABASE);
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
    void runOnEachNode_wrapsUnauthorizedExceptionInMongodbPermissionException() {
        // Given: A restricted user without profiling permissions
        MongoConnection restrictedConnection = createMongoConnection(RESTRICTED_USER, RESTRICTED_PASSWORD, TEST_DATABASE);
        MongodbConnectionResolver connectionResolver = host -> restrictedClient;
        MongodbClusterCommand clusterCommand = new MongodbClusterCommand(restrictedConnection, connectionResolver);

        // When & Then: Should wrap MongoDB's unauthorized error in MongodbPermissionException
        assertThatThrownBy(() -> clusterCommand.runOnEachNode(new Document("profile", 2)))
                .isInstanceOf(MongodbPermissionException.class)
                .hasMessageContaining("Permission denied")
                .hasCauseInstanceOf(MongoCommandException.class);
    }

    @Test
    void runOnEachNode_succeeds_whenUserHasPermissions() {
        // Given: An admin user with all permissions
        MongoConnection adminConnection = createMongoConnection(ADMIN_USER, ADMIN_PASSWORD, "admin");
        MongodbConnectionResolver connectionResolver = host -> adminClient;
        MongodbClusterCommand clusterCommand = new MongodbClusterCommand(adminConnection, connectionResolver);

        // When: Execute command with proper permissions
        var result = clusterCommand.runOnEachNode(new Document("profile", 0));

        // Then: Should succeed and return results
        assertThat(result).isNotEmpty();
        assertThat(result.values()).allSatisfy(doc -> {
            assertThat(doc).isInstanceOf(Document.class);
        });
    }

    @Nonnull
    private MongoConnection createMongoConnection(String username, String password, String authDatabase) {
        String host = mongoContainer.getHost();
        int port = mongoContainer.getFirstMappedPort();
        String uri = String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s",
                username, password, host, port, TEST_DATABASE, authDatabase);

        return new MongoConnection() {
            @Override
            public MongoClient connect() {
                return new MongoClient(uri);
            }

            @Override
            public DB getDatabase() {
                throw new UnsupportedOperationException("Not supported here.");
            }

            @Override
            public MongoDatabase getMongoDatabase() {
                return connect().getDatabase(TEST_DATABASE);
            }
        };
    }
}
