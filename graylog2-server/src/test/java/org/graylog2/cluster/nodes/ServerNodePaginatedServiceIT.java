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
package org.graylog2.cluster.nodes;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.system.ClusterResource;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;


class ServerNodePaginatedServiceIT {

    public static final int STALE_LEADER_TIMEOUT_MS = 2000;

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ServerNodePaginatedService serverNodePaginatedService;
    private SearchQueryParser queryParser;

    @BeforeEach
    void setUp() throws ValidationException, RepositoryException {

        mongodb.start();
        final ServerNodeClusterService serverNodeService = createServerNodeService();
        serverNodeService.registerServer(node());
        serverNodePaginatedService = new ServerNodePaginatedService(getMongoCollections());
        queryParser = new SearchQueryParser("hostname", ClusterResource.SERVER_NODE_ENTITY_SEARCH_MAPPINGS);
    }

    @Nonnull
    private ServerNodeClusterService createServerNodeService() throws RepositoryException, ValidationException {
        final Configuration configuration = configuration(Collections.singletonMap("stale_leader_timeout", String.valueOf(STALE_LEADER_TIMEOUT_MS)));
        return new ServerNodeClusterService(mongodb.mongoConnection(), configuration);
    }

    @Nonnull
    private MongoCollections getMongoCollections() {
        MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        return new MongoCollections(mongoJackObjectMapperProvider, mongodb.mongoConnection());
    }

    @AfterEach
    void tearDown() {
        mongodb.close();
    }

    private static NodeDto node() {
        return ServerNodeDto.Builder.builder()
                .setHostname("my-hostname")
                .setId(UUID.randomUUID().toString())
                .setLeader(true)
                .setTransportAddress("http://my-hostname:8999")
                .setProcessing(true)
                .setLoadBalancerStatus(LoadBalancerStatus.ALIVE)
                .build();
    }

    @Test
    void testPaginatedAccess() {
        final PaginatedList<ServerNodeDto> results = serverNodePaginatedService.searchPaginated(queryParser.parse(""), SortOrder.ASCENDING.toBsonSort("hostname"), 1, 1);
        Assertions.assertThat(results.delegate())
                .hasSize(1)
                .extracting(NodeDto::getHostname)
                .contains("my-hostname");
    }

    private Configuration configuration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final Configuration configuration = new Configuration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "password_secret", "thisisverysecretpassword",
                "root_password_sha2", "aaaaa",
                "data_dir", "/tmp"
        ));
        new JadConfig(List.of(mandatoryProps, new InMemoryRepository(properties)), configuration).process();
        return configuration;
    }
}
