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
package org.graylog.plugins.pipelineprocessor.simulator;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSortedSet;
import org.apache.commons.lang.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog2.plugin.Message;
import org.graylog2.shared.messageq.noop.NoopMessageQueueAcknowledger;
import org.graylog2.shared.metrics.MetricRegistryFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RuleSimulator {

    private final ConfigurationStateUpdater configurationStateUpdater;
    private final ObjectMapper objectMapper;

    @Inject
    public RuleSimulator(ConfigurationStateUpdater configurationStateUpdater, ObjectMapper objectMapper) {
        this.configurationStateUpdater = configurationStateUpdater;
        this.objectMapper = objectMapper;
    }

    public Message simulate(Rule rule, Message message) {
        Stage stage = Stage.builder()
                .stage(0)
                .ruleReferences(Collections.emptyList())
                .match(Stage.Match.PASS)
                .build();
        stage.setRules(List.of(rule));
        Pipeline pipeline = Pipeline.builder()
                .stages(ImmutableSortedSet.of(stage))
                .name("dummyPipeline")
                .id(UUID.randomUUID().toString())
                .build();
        stage.setPipeline(pipeline);
        PipelineInterpreter pipelineInterpreter = new PipelineInterpreter(
                new NoopMessageQueueAcknowledger(), MetricRegistryFactory.create(), configurationStateUpdater);
        final PipelineInterpreterTracer pipelineInterpreterTracer = new PipelineInterpreterTracer();
        pipelineInterpreter.evaluateStage(stage, message, message.getId(),
                new ArrayList<>(), Collections.emptySet(),
                pipelineInterpreterTracer.getSimulatorInterpreterListener());
        return message;
    }

    public Message createMessage(String messageString) {
        Message message;
        try {
            Map<String, Object> map = objectMapper.readValue(messageString, Map.class);
            if (!map.containsKey("_id")) {
                map.put("_id", UUID.randomUUID().toString());
            }
            message = new Message(map);
        } catch (JacksonException e) {
            message = new Message(messageString, "127.0.0.1", DateTime.now(DateTimeZone.UTC));
            if (StringUtils.startsWith(StringUtils.trim(messageString), "{")) {
                message.addField("gl2_simulator_json_error",
                        "Cannot parse simulation message as JSON. Using as raw message field instead: " + e.getMessage());
            }
        }

        return message;
    }
}
