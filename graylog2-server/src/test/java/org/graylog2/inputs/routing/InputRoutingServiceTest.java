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
package org.graylog2.inputs.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineResource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineUtils;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputRoutingService;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputRoutingServiceTest {
    final StreamService streamService = mock(StreamService.class);
    final InputService inputService = mock(InputService.class);
    final RuleService ruleService = mock(RuleService.class);
    final PipelineService pipelineService = mock(PipelineService.class);
    final PipelineRuleParser pipelineRuleParser = mock(PipelineRuleParser.class);
    final EventBus eventBus = mock(EventBus.class);
    final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    InputRoutingService inputRoutingService;

    static final String INPUT_ID = "inputId";
    static final String INPUT_NAME = "inputName";
    static final String INPUT_NEW_NAME = "inputNewName";
    static final String STREAM_ID = "streamId";
    static final String STREAM_NAME = "streamName";

    @BeforeEach
    void setUp() {
        inputRoutingService = new InputRoutingService(
                ruleService, inputService, streamService, pipelineService, pipelineRuleParser, eventBus);
    }

    @Test
    void createRule() throws IOException, NotFoundException {
        final Stream stream = mock(Stream.class);
        when(stream.getTitle()).thenReturn(STREAM_NAME);
        when(streamService.load(STREAM_ID)).thenReturn(stream);

        final Input input = mock(Input.class);
        when(input.getId()).thenReturn(INPUT_ID);
        when(input.getTitle()).thenReturn(INPUT_NAME);
        when(inputService.find(INPUT_ID)).thenReturn(input);

        when(ruleService.findByName(anyString())).thenReturn(Optional.empty());
        when(ruleService.save(any())).thenAnswer(i -> i.getArguments()[0]);

        final RuleDao expected = loadFixture("org/graylog2/inputs/routing/InputRoutingRule1.json", RuleDao.class);

        RuleDao actual = inputRoutingService.createRoutingRule(new PipelineResource.RoutingRequest(INPUT_ID, STREAM_ID, false));

        assertThat(actual.title()).isEqualTo(expected.title());
        assertThat(actual.description()).isEqualTo(expected.description());
        assertThat(actual.source()).isEqualTo(expected.source());
    }

    @Test
    void handleInputDeleted() throws IOException {
        final RuleDao ruleDao = loadFixture("org/graylog2/inputs/routing/InputRoutingRule1.json", RuleDao.class);
        when(ruleService.loadAll()).thenReturn(List.of(ruleDao));

        inputRoutingService.handleInputDeleted(new InputDeletedEvent(INPUT_ID, INPUT_NAME));
        verify(ruleService).delete(ruleDao);
    }

    @Test
    void deleteFromDefaultPipeline() throws IOException, NotFoundException {
        final RuleDao ruleDao = loadFixture("org/graylog2/inputs/routing/InputRoutingRule1.json", RuleDao.class);
        when(ruleService.loadAll()).thenReturn(List.of(ruleDao));

        when(pipelineService.loadByName(anyString())).thenReturn(PipelineDao.builder().id("pipelineId1").title("pipelineDao").source("dummySource").build());

        final PipelineSource pipelineSource = loadFixture("org/graylog2/inputs/routing/InputRoutingPipelineSource1.json", PipelineSource.class);
        Assertions.assertThat(pipelineSource.stages().get(0).rules()).hasSize(1);

        try (final MockedStatic<PipelineSource> pipelineSourceMockedStatic = mockStatic(PipelineSource.class);
             final MockedStatic<PipelineUtils> pipelineUtilsMockedStatic = mockStatic(PipelineUtils.class)
        ) {
            pipelineSourceMockedStatic.when(() -> PipelineSource.fromDao(any(), any())).thenReturn(pipelineSource);
            pipelineUtilsMockedStatic.when(() -> PipelineUtils.createPipelineString(any())).thenReturn(pipelineSource.source());

            inputRoutingService.handleRuleDeleted(RulesChangedEvent.deletedRule(ruleDao.id(), ruleDao.title()));

            Assertions.assertThat(pipelineSource.stages().get(0).rules()).isEmpty();
        }
    }

    @Test
    void ignoreOtherInputDeleted() {
        inputRoutingService.handleInputDeleted(new InputDeletedEvent("id", "dummyInput"));
        verify(ruleService, times(0)).delete(any(RuleDao.class));
    }

    @Test
    void handleInputRenamed() throws IOException, NotFoundException {
        final RuleDao ruleDao = loadFixture("org/graylog2/inputs/routing/InputRoutingRule1.json", RuleDao.class);
        when(ruleService.loadAll()).thenReturn(List.of(ruleDao));

        String pipelineSource = "pipeline \"All Messages Routing\"\nstage 0 match EITHER\nrule \"gl_route_inputName_to_streamName\"\nend";
        when(pipelineService.loadByName(anyString())).thenReturn(PipelineDao.builder().id("pipelineId1").title("pipelineDao").source(pipelineSource).build());

        inputRoutingService.handleInputRenamed(new InputRenamedEvent(INPUT_ID, INPUT_NAME, INPUT_NEW_NAME));

        verify(ruleService).save(argThat(ruleDao1 -> ruleDao1.source().contains(INPUT_NEW_NAME)), anyBoolean());
        verify(ruleService, times(1)).save(any(), anyBoolean());
    }

    @Test
    void ignoreOtherInputRenamed() {
        inputRoutingService.handleInputRenamed(new InputRenamedEvent("id", "dummyInput", "dummyInputNew"));
        verify(ruleService, times(0)).save(any());
    }

    private <T> T loadFixture(String path, Class<T> clazz) throws IOException {
        return objectMapper.readValue(
                IOUtils.toString(Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8), clazz);
    }

}
