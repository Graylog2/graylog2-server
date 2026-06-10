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
package org.graylog.storage.opensearch3.ism;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.indexer.datastream.policy.IsmPolicy;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IsmApi {

    private final ObjectMapper objectMapper;
    private final OfficialOpensearchClient client;
    private final OpenSearchGenericClient genericClient;

    @Inject
    public IsmApi(OfficialOpensearchClient client, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = client;
        genericClient = client.sync().generic();
    }

    public Optional<IsmPolicy> getPolicy(String policyId) {
        Request request = os3request("GET", "policies/" + policyId, null);
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
        final Request request = os3request("PUT", "policies/" + policyId, policy);
        perform(request,
                new TypeReference<IsmPolicy>() {},
                "Unable to create ism policy");
    }

    public void addPolicyToIndex(String policyId, String index) {
        final Request request = os3request("POST", "add/" + index, Map.of("policy_id", policyId));
        perform(request,
                new TypeReference<JsonNode>() {},
                "Unable to add policy to index");
    }

    public void removePolicyFromIndex(String index) {
        final Request request = os3request("POST", "remove/" + index, null);
        perform(request,
                new TypeReference<JsonNode>() {},
                "Unable to remove policy from index");
    }

    public void deletePolicy(String policyId) {
        final Request request = os3request("DELETE", "policies/" + policyId, null);
        perform(request,
                new TypeReference<JsonNode>() {},
                "Unable to delete policy");
    }

    private <R> Optional<R> perform(Request request, TypeReference<R> responseClass, String errorMessage) {
        // it would be nice if we could use the provided IsmClient, but this is currently not possible due to
        // https://github.com/opensearch-project/opensearch-java/issues/1717 (fix submitted)
        return client.execute(() -> {
            Response response = genericClient.execute(request);
            if (response.getStatus() == 404) {
                return Optional.empty();
            }
            return response.getBody()
                    .map(Body::bodyAsString)
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, responseClass);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }, errorMessage);
    }

    private Request os3request(String method, String endpoint, Object body) {
        try {
            return Requests.builder()
                    .method(method)
                    .endpoint("/_plugins/_ism/" + endpoint)
                    .query(Map.of("format", "json"))
                    .json(objectMapper.writeValueAsString(body))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
