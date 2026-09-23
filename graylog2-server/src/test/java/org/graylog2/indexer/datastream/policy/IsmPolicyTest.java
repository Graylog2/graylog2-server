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
package org.graylog2.indexer.datastream.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog2.indexer.datastream.policy.actions.Action;
import org.graylog2.indexer.datastream.policy.actions.DeleteAction;
import org.graylog2.indexer.datastream.policy.actions.Retry;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IsmPolicyTest {

    public static IsmPolicy createSimpleTestPolicy() {
        Policy.State deleteState = deleteState();
        Policy.State initialState = transitionState(deleteState.name());
        Policy policy = new Policy("Test Policy", initialState.name(), ImmutableList.of(initialState, deleteState));
        return new IsmPolicy("graylog-ism-test-policy", policy);
    }

    /**
     * Same as {@link #createSimpleTestPolicy()}, but with an ISM template, which is what makes OpenSearch attach the
     * policy to backing indices created by later data stream rollovers.
     */
    public static IsmPolicy createTestPolicyWithIsmTemplate(String policyId, String indexPattern) {
        Policy.State deleteState = deleteState();
        Policy.State initialState = transitionState(deleteState.name());
        Policy policy = new Policy(null, "Test Policy", null, initialState.name(),
                ImmutableList.of(initialState, deleteState),
                ImmutableList.of(new Policy.IsmTemplate(ImmutableList.of(indexPattern), 100)));
        return new IsmPolicy(policyId, policy);
    }

    private static Policy.State transitionState(String nextState) {
        final List<Action> actions = ImmutableList.of();
        final List<Policy.Transition> transitions = ImmutableList.of(
                new Policy.Transition(nextState, new Policy.Condition("1d"))
        );
        return new Policy.State("transition", actions, transitions);
    }

    private static Policy.State deleteState() {
        final List<Action> actions = ImmutableList.of(new Action(new Retry(3, "exponential", "1s"), new DeleteAction()));
        final List<Policy.Transition> transitions = ImmutableList.of();
        return new Policy.State("delete", actions, transitions);
    }

    String simpleTestPolicyJson = """
            {
              "_id" : "graylog-ism-test-policy",
              "policy" : {
                "description" : "Test Policy",
                "default_state" : "transition",
                "states" : [ {
                  "name" : "transition",
                  "actions" : [ ],
                  "transitions" : [ {
                    "state_name" : "delete",
                    "conditions" : {
                      "min_index_age" : "1d"
                    }
                  } ]
                }, {
                  "name" : "delete",
                  "actions" : [ {
                    "retry" : {
                      "count" : 3,
                      "backoff" : "exponential",
                      "delay" : "1s"
                    },
                    "delete" : { }
                  } ],
                  "transitions" : [ ]
                } ]
              }
            }""";

    String ismTemplatePolicyJson = """
            {
              "_id" : "graylog-ism-test-policy",
              "policy" : {
                "description" : "Test Policy",
                "last_updated_time" : 1755678598000,
                "last_updated_time_in_millis" : "2026-08-20T08:29:58.000Z",
                "default_state" : "delete",
                "states" : [ {
                  "name" : "delete",
                  "actions" : [ {
                    "delete" : { }
                  } ],
                  "transitions" : [ ]
                } ],
                "ism_template" : [ {
                  "index_patterns" : [ "test-data-stream" ],
                  "priority" : 100,
                  "last_updated_time" : 1755678598000,
                  "last_updated_time_in_millis" : "2026-08-20T08:29:58.000Z"
                } ]
              }
            }""";


    @Test
    public void testPolicySerializationWorks() throws IOException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createSimpleTestPolicy());
        assertThat(objectMapper.readTree(s)).isEqualTo(objectMapper.readTree(simpleTestPolicyJson));
    }

    @Test
    public void testPolicyDeserializationWorks() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final IsmPolicy policy = objectMapper.readValue(simpleTestPolicyJson, IsmPolicy.class);
        assertThat(policy.policy().states()).hasSize(2);
    }

    @Test
    public void testIsmTemplateSerializationWorks() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = objectMapper.writeValueAsString(
                createTestPolicyWithIsmTemplate("graylog-ism-test-policy", "test-data-stream"));

        assertThat(objectMapper.readTree(json).at("/policy/ism_template/0/index_patterns/0").asText())
                .isEqualTo("test-data-stream");
        assertThat(objectMapper.readTree(json).at("/policy/ism_template/0/priority").asInt()).isEqualTo(100);
    }

    /**
     * OpenSearch always returns {@code ism_template} as an array and adds a {@code last_updated_time} to each entry,
     * even when the policy was created with a single template object.
     */
    @Test
    public void testIsmTemplateDeserializationWorks() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final IsmPolicy policy = objectMapper.readValue(ismTemplatePolicyJson, IsmPolicy.class);

        assertThat(policy.policy().ismTemplate()).singleElement().satisfies(ismTemplate -> {
            assertThat(ismTemplate.indexPatterns()).containsExactly("test-data-stream");
            assertThat(ismTemplate.priority()).isEqualTo(100);
        });
    }

}
