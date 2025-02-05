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
package org.graylog2.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.apache.commons.io.IOUtils;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineResource;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputRoutingServiceTest {
    final StreamService streamService = mock(StreamService.class);
    final InputService inputService = mock(InputService.class);
    final RuleService ruleService = mock(RuleService.class);

    @Test
    void createRule() throws IOException, NotFoundException {
        final String INPUT_ID = "inputId";
        final String INPUT_NAME = "inputName";
        final String STREAM_ID = "streamId";
        final String STREAM_NAME = "streamName";

        final Stream stream = mock(Stream.class);
        when(stream.getTitle()).thenReturn(STREAM_NAME);
        when(streamService.load(STREAM_ID)).thenReturn(stream);

        final Input input = mock(Input.class);
        when(input.getTitle()).thenReturn(INPUT_NAME);
        when(inputService.find(INPUT_ID)).thenReturn(input);

        when(ruleService.findByName(anyString())).thenReturn(Optional.empty());
        when(ruleService.save(any())).thenAnswer(i -> i.getArguments()[0]);

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String ruleDaoFixture = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("org/graylog2/inputs/InputRoutingRule1.json"), "UTF-8");
        final RuleDao expected = objectMapper.readValue(ruleDaoFixture, RuleDao.class);

        InputRoutingService inputRoutingService = new InputRoutingService(
                ruleService, inputService, streamService, null,
                mock(PipelineRuleParser.class), mock(EventBus.class));
        RuleDao actual = inputRoutingService.createRoutingRule(new PipelineResource.RoutingRequest(INPUT_ID, STREAM_ID, false));

        assertThat(actual.title()).isEqualTo(expected.title());
        assertThat(actual.description()).isEqualTo(expected.description());
        assertThat(actual.source()).isEqualTo(expected.source());
    }
}
