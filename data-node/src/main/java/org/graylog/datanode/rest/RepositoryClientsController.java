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
package org.graylog.datanode.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch.generic.Requests;

import java.util.Map;

/**
 * Shows the snapshot repositories and repository clients as the opensearch cluster sees them.
 */
@Path("/repository-clients")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryClientsController {

    private final OpensearchProcess process;

    @Inject
    public RepositoryClientsController(OpensearchProcess process) {
        this.process = process;
    }

    @GET
    public RepositoryClientsResponse repositoryClients() {
        final OfficialOpensearchClient client = process.openSearchClient()
                .orElseThrow(() -> new ServiceUnavailableException("OpenSearch is not running"));
        return RepositoryClientsResponse.from(
                get(client, "/_snapshot", Map.of()),
                get(client, "/_nodes/settings", Map.of("flat_settings", "true", "filter_path", "nodes.*.name,nodes.*.settings")));
    }

    private static JsonNode get(OfficialOpensearchClient client, String endpoint, Map<String, String> parameters) {
        return client.performRequest(Requests.builder().endpoint(endpoint).method("GET").query(parameters).build(),
                "Failed to read the repository clients from opensearch");
    }
}
