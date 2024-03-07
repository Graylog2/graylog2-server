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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.apache.http.HttpEntity;
import org.graylog.shaded.opensearch2.org.apache.http.entity.ContentType;
import org.graylog.shaded.opensearch2.org.apache.http.entity.StringEntity;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog2.indexer.security.SecurityAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SecurityAdapterOS implements SecurityAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityAdapterOS.class);

    final private PlainJsonApi jsonApi;
    final private OpenSearchClient client;
    private final ObjectMapper objectMapper;

    @Inject
    public SecurityAdapterOS(final OpenSearchClient client,
                             final ObjectMapper objectMapper,
                             final PlainJsonApi jsonApi) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.jsonApi = jsonApi;
    }

    @Override
    public Mapping getMappingForRole(final String role) {
        final JsonNode result = jsonApi.perform(request("GET", "rolesmapping/" + role), "Unable to retrieve role mapping for role " + role);

        final JsonNode fields = result.path(role);
        try {
            return objectMapper.treeToValue(fields, Mapping.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public MappingResponse addUserToRoleMapping(final String role, final String user) {
        final var mapping = getMappingForRole(role);
        if(!mapping.users().contains(user)) {
            final List<String> users = new ArrayList<>();
            users.addAll(mapping.users());
            users.add(user);
            return setUserToRoleMapping(role, new Mapping(mapping.backendRoles(), mapping.hosts(), users));
        } else {
            return new MappingResponse("OK", "User already in mapping");
        }
    }

    public MappingResponse removeUserFromRoleMapping(final String role, final String user) {
        final var mapping = getMappingForRole(role);
        if(mapping.users().contains(user)) {
            final var users = mapping.users().stream().filter(u -> !u.equals(user)).toList();
            return setUserToRoleMapping(role, new Mapping(mapping.backendRoles(), mapping.hosts(), users));
        } else {
            return new MappingResponse("OK", "User did not exist in mapping");
        }
    }

    private MappingResponse setUserToRoleMapping(final String role, final Mapping mapping) {

        try {
            final JsonNode result = jsonApi.perform(
                    request("PUT", "rolesmapping/" + role,
                            new StringEntity(objectMapper.writeValueAsString(mapping), ContentType.APPLICATION_JSON)
                    ),
                    "Could not set role mapping for role " + role);

            return new MappingResponse("OK", "tbd");
        } catch (JsonProcessingException ex) {
            LOG.error("Could not send Request: {}", ex.getMessage(), ex);
            return new MappingResponse("ERROR", ex.getMessage());
        }
    }

    private Request request(@SuppressWarnings("SameParameterValue") String method, String endpoint) {
        return this.request(method, endpoint, null);
    }

    private Request request(@SuppressWarnings("SameParameterValue") String method, String endpoint, HttpEntity entity) {
        final Request request = new Request(method, "/_plugins/_security/api/" + endpoint);
        request.addParameter("format", "json");
        if(entity != null) {
            request.setEntity(entity);
        }
        return request;
    }
}
