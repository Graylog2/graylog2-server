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
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.Configuration;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.system.ClusterResource;
import org.graylog2.search.SearchQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@ExtendWith(MongoDBExtension.class)
class ServerNodePaginatedServiceIT {

    public static final int STALE_LEADER_TIMEOUT_MS = 2000;

    private ServerNodePaginatedService serverNodePaginatedService;
    private SearchQueryParser queryParser;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) throws ValidationException, RepositoryException {
        final Configuration configuration = configuration(Collections.singletonMap("stale_leader_timeout", String.valueOf(STALE_LEADER_TIMEOUT_MS)));
        final ServerNodeClusterService serverNodeService = new ServerNodeClusterService(mongoCollections.mongoConnection(), configuration);
        serverNodeService.registerServer(node());
        serverNodePaginatedService = new ServerNodePaginatedService(mongoCollections);
        queryParser = new SearchQueryParser("hostname", ClusterResource.SERVER_NODE_ENTITY_SEARCH_MAPPINGS);
    }

    private static NodeDto node() {
        return ServerNodeDto.Builder.builder()
                .setHostname("my-hostname")
                .setId(UUID.randomUUID().toString())
                .setLeader(true)
                .setTransportAddress("http://my-hostname:8999")
                .setProcessing(true)
                .setLifecycle(Lifecycle.RUNNING)
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
