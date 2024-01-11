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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog.storage.opensearch2.ism.policy.Policy;
import org.graylog.storage.opensearch2.ism.policy.actions.Action;
import org.graylog.storage.opensearch2.ism.policy.actions.RolloverAction;
import org.graylog.storage.opensearch2.ism.policy.actions.RollupAction;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class PolicyTest {

    @Test
    public void testPolicyCreation() throws IOException {

        final List<Action> actions1 = ImmutableList.of(new Action(new RolloverAction("1d")));
        final List<Policy.Transition> transitions1 = ImmutableList.of(new Policy.Transition("state2", null));

        List<RollupAction.IsmRollup.Dimension> dimensions = ImmutableList.of(new RollupAction.IsmRollup.DateHistogram("timestamp", "60m", "America/Los_Angeles"));
        List<RollupAction.IsmRollup.Metric> metrics = ImmutableList.of(new RollupAction.IsmRollup.Metric("aggField", ImmutableList.of(new RollupAction.IsmRollup.AvgMetric())));
        final RollupAction.IsmRollup ismRollup = new RollupAction.IsmRollup("targetIdx", "rollupDescription", 200, dimensions, metrics);
        final List<Action> actions2 = ImmutableList.of(new Action(new RollupAction(ismRollup)));
        final List<Policy.Transition> transitions2 = ImmutableList.of(new Policy.Transition("end", new Policy.Condition("14d")));
        List<Policy.State> states = ImmutableList.of(
                new Policy.State("state1", actions1, transitions1),
                new Policy.State("state2", actions2, transitions2)
        );
        Policy policy = new Policy(null, "testpolicy", null, "state1", states);
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policy);
        System.out.println(s);
    }

    @Test
    public void testPolicyDeserialization() throws JsonProcessingException {
        String json = "{\n" +
                "  \"description\" : \"testpolicy\",\n" +
                "  \"default_state\" : \"state1\",\n" +
                "  \"states\" : [ {\n" +
                "    \"name\" : \"state1\",\n" +
                "    \"actions\" : [ {\n" +
                "      \"rollover\" : {\n" +
                "        \"min_index_age\" : \"1d\"\n" +
                "      }\n" +
                "    } ],\n" +
                "    \"transitions\" : [ {\n" +
                "      \"state_name\" : \"state2\"\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"name\" : \"state2\",\n" +
                "    \"actions\" : [ {\n" +
                "      \"rollup\" : {\n" +
                "        \"ism_rollup\" : {\n" +
                "          \"target_index\" : \"targetIdx\",\n" +
                "          \"description\" : \"rollupDescription\",\n" +
                "          \"page_size\" : 200,\n" +
                "          \"dimensions\" : [ {\n" +
                "            \"date_histogram\" : {\n" +
                "              \"source_field\" : \"timestamp\",\n" +
                "              \"fixed_interval\" : \"60m\",\n" +
                "              \"timezone\" : \"America/Los_Angeles\"\n" +
                "            }\n" +
                "          } ],\n" +
                "          \"metrics\" : [ {\n" +
                "            \"source_field\" : \"aggField\",\n" +
                "            \"metrics\" : [ {\n" +
                "              \"avg\" : { }\n" +
                "            } ]\n" +
                "          } ]\n" +
                "        }\n" +
                "      }\n" +
                "    } ],\n" +
                "    \"transitions\" : [ {\n" +
                "      \"state_name\" : \"end\",\n" +
                "      \"conditions\" : {\n" +
                "        \"min_index_age\" : \"14d\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  } ]\n" +
                "}";

        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final Policy policy = objectMapper.readValue(json, Policy.class);

    }

}
