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
package org.graylog.storage.opensearch2.ism.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog.storage.opensearch2.ism.policy.actions.Action;
import org.graylog.storage.opensearch2.ism.policy.actions.DeleteAction;
import org.graylog.storage.opensearch2.ism.policy.actions.Retry;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

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


    @Test
    public void testPolicySerializationWorks() throws IOException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createSimpleTestPolicy());
        assertThat(s).isEqualTo(simpleTestPolicyJson);
    }

    @Test
    public void testPolicyDeserializationWorks() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final IsmPolicy policy = objectMapper.readValue(simpleTestPolicyJson, IsmPolicy.class);
        assertThat(policy.policy().states()).hasSize(2);
    }

}
