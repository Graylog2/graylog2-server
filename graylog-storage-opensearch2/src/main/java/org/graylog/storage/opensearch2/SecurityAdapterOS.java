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
import org.apache.commons.collections4.ListUtils;
import org.graylog.shaded.opensearch2.org.apache.http.HttpEntity;
import org.graylog.shaded.opensearch2.org.apache.http.entity.ContentType;
import org.graylog.shaded.opensearch2.org.apache.http.entity.StringEntity;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog2.indexer.security.SecurityAdapter;
import org.graylog2.indexer.security.SecurityRole;
import org.graylog2.indexer.security.PrincipalRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * see: https://opensearch.org/docs/latest/security/access-control/api/
 */
public class SecurityAdapterOS implements SecurityAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityAdapterOS.class);

    final private PlainJsonApi jsonApi;
    private final ObjectMapper objectMapper;

    @Inject
    public SecurityAdapterOS(final ObjectMapper objectMapper,
                             final PlainJsonApi jsonApi) {
        this.objectMapper = objectMapper;
        this.jsonApi = jsonApi;
    }

    @Override
    public Mapping getMappingForRole(final String role) {
        final JsonNode result = jsonApi.perform(securityApiRequest("GET", "rolesmapping/" + role), "Unable to retrieve role mapping for role " + role);

        final JsonNode fields = result.path(role);
        try {
            return objectMapper.treeToValue(fields, Mapping.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public MappingResponse addUserToRoleMapping(final String role, final String user) {
        final var mapping = getMappingForRole(role);
        if (!mapping.users().contains(user)) {
            final List<String> users = new ArrayList<>(mapping.users());
            users.add(user);
            return setUserToRoleMapping(role, new Mapping(mapping.backendRoles(), mapping.hosts(), users));
        } else {
            return new MappingResponse("OK", "User already in mapping");
        }
    }

    public MappingResponse removeUserFromRoleMapping(final String role, final String user) {
        final var mapping = getMappingForRole(role);
        if (mapping.users().contains(user)) {
            final var users = mapping.users().stream().filter(u -> !u.equals(user)).toList();
            return setUserToRoleMapping(role, new Mapping(mapping.backendRoles(), mapping.hosts(), users));
        } else {
            return new MappingResponse("OK", "User did not exist in mapping");
        }
    }

    @Override
    public List<SecurityRole> getRoles() {
        final JsonNode response = jsonApi.perform(securityApiRequest("GET", "roles"), "Unable to retrieve roles");
        List<SecurityRole> roles = new ArrayList<>();

        final Iterator<Map.Entry<String, JsonNode>> elements = response.fields();

        while (elements.hasNext()) {
            final Map.Entry<String, JsonNode> roleNode = elements.next();
            final String roleName = roleNode.getKey();
            final JsonNode roleValue = roleNode.getValue();
            final String description = Optional.ofNullable(roleValue.get("description")).map(JsonNode::asText).orElse(null);
            roles.add(new SecurityRole(roleName, description));
        }
        return roles;
    }

    @Override
    public List<PrincipalRoles> getPrincipals() {
        final JsonNode response = jsonApi.perform(securityApiRequest("GET", "rolesmapping/"), "Unable to retrieve roles mapping");
        final Map<String, List<String>> principalToRoles = parseRoles(response);
        return principalToRoles.entrySet().stream().map(entry -> new PrincipalRoles(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PrincipalRoles::principal))
                .collect(Collectors.toList());
    }

    private static Map<String, List<String>> parseRoles(JsonNode response) {

        final Iterator<Map.Entry<String, JsonNode>> elements = response.fields();
        Map<String, List<String>> usersToRoles = new LinkedHashMap<>();
        while (elements.hasNext()) {
            final Map.Entry<String, JsonNode> roleNode = elements.next();
            final String roleName = roleNode.getKey();
            final JsonNode users = roleNode.getValue().get("users");
            users.elements().forEachRemaining(userNode -> usersToRoles.compute(userNode.asText(), (user, roles) -> roles == null ? Collections.singletonList(roleName) : ListUtils.union(roles, Collections.singletonList(roleName))));
        }
        return usersToRoles;
    }

    private MappingResponse setUserToRoleMapping(final String role, final Mapping mapping) {
        try {
            final JsonNode result = jsonApi.perform(
                    securityApiRequest("PUT", "rolesmapping/" + role,
                            new StringEntity(objectMapper.writeValueAsString(mapping), ContentType.APPLICATION_JSON)
                    ),
                    "Could not set role mapping for role " + role);

            var retval = new MappingResponse(result.get("status").asText(), result.get("message").asText());
            return retval;
        } catch (JsonProcessingException ex) {
            LOG.error("Could not send Request: {}", ex.getMessage(), ex);
            return new MappingResponse("ERROR", ex.getMessage());
        }
    }

    private Request securityApiRequest(@SuppressWarnings("SameParameterValue") String method, String endpoint) {
        return this.securityApiRequest(method, endpoint, null);
    }

    private Request securityApiRequest(@SuppressWarnings("SameParameterValue") String method, String endpoint, HttpEntity entity) {
        final Request request = new Request(method, "/_plugins/_security/api/" + endpoint);
        request.addParameter("format", "json");
        if (entity != null) {
            request.setEntity(entity);
        }
        return request;
    }
}
