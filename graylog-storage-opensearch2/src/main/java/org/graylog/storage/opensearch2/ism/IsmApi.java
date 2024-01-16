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
package org.graylog.storage.opensearch2.ism;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.ResponseException;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.ism.policy.IsmPolicy;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

public class IsmApi {

    private final ObjectMapper objectMapper;
    private final OpenSearchClient client;

    @Inject
    public IsmApi(OpenSearchClient client, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public Optional<IsmPolicy> getPolicy(String policyId) {
        final Request request = request("GET", "policies/" + policyId, null);
        return perform(request, new TypeReference<>() {}, "Could not get ism policy");
    }

    public void createPolicy(String policyId, IsmPolicy policy) {
        // remove id from policy since it is not allowed on creation
        if (!Objects.isNull(policy.id())) {
            if (!policyId.equals(policy.id())) {
                throw new IllegalArgumentException("Policy id present in policy does not match provided id.");
            }
            policy = new IsmPolicy(policy.policy());
        }
        final Request request = request("PUT", "policies/" + policyId, policy);
        perform(request,
                new TypeReference<IsmPolicy>() {},
                "Unable to create ism policy");
    }

    public void addPolicyToIndex(String policyId, String index) {
        final Request request = request("POST", "add/" + index, null);
        request.setJsonEntity(f("{\"policy_id\":\"%s\"}", policyId));
        perform(request,
                new TypeReference<JsonNode>() {},
                "Unable to add policy to index");
    }

    public void removePolicyFromIndex(String index) {
        final Request request = request("POST", "remove/" + index, null);
        perform(request,
                new TypeReference<JsonNode>() {},
                "Unable to remove policy from index");
    }

    public void deletePolicy(String policyId) {
        final Request request = request("DELETE", "policies/" + policyId, null);
        perform(request,
                new TypeReference<JsonNode>() {},
                "Unable to delete policy");
    }

    private <R> Optional<R> perform(Request request, TypeReference<R> responseClass, String errorMessage) {
        return client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);

            Response response;
            try {
                response = c.getLowLevelClient().performRequest(request);
            } catch (ResponseException e) {
                if (e.getResponse().getStatusLine().getStatusCode() == 404)
                    return Optional.empty();
                throw e;
            }
            return Optional.of(returnType(response, responseClass));
        }, errorMessage);
    }

    private <R> R returnType(Response response, TypeReference<R> responseClass) throws IOException {
        return objectMapper.readValue(response.getEntity().getContent(), responseClass);
    }

    private Request request(@SuppressWarnings("SameParameterValue") String method, String endpoint, Object body) {
        final Request request = new Request(method, "/_plugins/_ism/" + endpoint);
        request.addParameter("format", "json");
        try {
            request.setJsonEntity(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return request;
    }

}
