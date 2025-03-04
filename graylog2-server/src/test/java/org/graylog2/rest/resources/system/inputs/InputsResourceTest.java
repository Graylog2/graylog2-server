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
package org.graylog2.rest.resources.system.inputs;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.BadRequestException;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.diagnosis.InputDiagnosticService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputsResourceTest {

    @Mock
    InputService inputService;

    @Mock
    StreamService streamService;

    @Mock
    StreamRuleService streamRuleService;

    @Mock
    PipelineService pipelineService;

    @Mock
    PipelineStreamConnectionsService pipelineStreamConnectionsService;

    @Mock
    MessageInputFactory messageInputFactory;

    @Mock
    Configuration configuration;

    @Mock
    MessageInput messageInput;

    InputsResource inputsResource;

    @BeforeEach
    public void setUp() {
        inputsResource = new InputsTestResource(inputService, streamService, streamRuleService,
                pipelineService, pipelineStreamConnectionsService, messageInputFactory, configuration);
    }

    @Test
    void testCreateNotGlobalInputInCloud() {
        when(configuration.isCloud()).thenReturn(true);

        assertThatThrownBy(() -> inputsResource.create(getCR(false))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only global inputs");
    }

    @Test
    void testCreateNotCloudCompatibleInputInCloud() throws Exception {
        when(configuration.isCloud()).thenReturn(true);
        when(messageInput.isCloudCompatible()).thenReturn(false);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);

        assertThatThrownBy(() -> inputsResource.create(getCR(true))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed in the cloud environment");
    }

    @Test
    void testCreateCloudCompatibleInputInCloud() throws Exception {
        when(configuration.isCloud()).thenReturn(true);
        when(messageInput.isCloudCompatible()).thenReturn(true);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);
        when(inputService.save(any())).thenReturn("id");

        assertThat(inputsResource.create(getCR(true)).getStatus()).isEqualTo(201);
    }

    @Test
    void testCreateInput() throws Exception {
        when(configuration.isCloud()).thenReturn(false);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);
        when(inputService.save(any())).thenReturn("id");

        assertThat(inputsResource.create(getCR(true)).getStatus()).isEqualTo(201);
    }

    @Test
    void testStreamReferences() {
        when(streamRuleService.loadForInput("inputId")).thenReturn(List.of(
                new StreamRuleMock(Map.of("_id", "ruleId1", "stream_id", "streamId1")),
                new StreamRuleMock(Map.of("_id", "ruleId2", "stream_id", "streamId2"))
        ));
        when(streamService.streamTitleFromCache("streamId1")).thenReturn("streamTitle1");
        when(streamService.streamTitleFromCache("streamId2")).thenReturn("streamTitle2");
        when(pipelineService.loadBySourcePattern("inputId")).thenReturn(Collections.emptyList());

        final List<InputsResource.InputReference> expected = List.of(
                new InputsResource.InputReference("streamId1", "streamTitle1"),
                new InputsResource.InputReference("streamId2", "streamTitle2"));
        final InputsResource.InputReferences refs = inputsResource.getReferences("inputId");

        assertThat(refs.streamRefs()).hasSize(2);
        assertThat(refs.streamRefs()).containsAll(expected);
    }

    @Test
    void testPipelineReferences() {
        when(streamRuleService.loadForInput("inputId")).thenReturn(Collections.emptyList());
        when(pipelineService.loadBySourcePattern("inputId")).thenReturn(
                List.of(PipelineDao.builder().id("pipelineId1").title("pipelineTitle1").source("source1").build(),
                        PipelineDao.builder().id("pipelineId2").title("pipelineTitle2").source("source2").build())
        );
        when(pipelineStreamConnectionsService.loadByPipelineId("pipelineId1")).thenReturn(
                Set.of(PipelineConnections.create("id1", "streamId1", Set.of("pipelineId1", "pipelineId99")))
        );
        when(pipelineStreamConnectionsService.loadByPipelineId("pipelineId2")).thenReturn(Collections.emptySet());

        final InputsResource.InputReferences refs = inputsResource.getReferences("inputId");

        assertThat(refs.pipelineRefs()).hasSize(1);
    }

    private InputCreateRequest getCR(boolean global) {
        return InputCreateRequest.builder()
                .global(global)
                .title("myTitle")
                .configuration(new HashMap<>())
                .type("myType")
                .build();
    }

    static class InputsTestResource extends InputsResource {

        private final User user;
        private final Subject subject;

        public InputsTestResource(InputService inputService,
                                  StreamService streamService,
                                  StreamRuleService streamRuleService,
                                  PipelineService pipelineService,
                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                  MessageInputFactory messageInputFactory,
                                  Configuration config) {
            super(inputService, mock(InputDiagnosticService.class), streamService, streamRuleService,
                    pipelineService, messageInputFactory, config, mock(ClusterEventBus.class));
            configuration = mock(HttpConfiguration.class);

            this.user = mock(User.class);
            lenient().when(user.getName()).thenReturn("foo");
            lenient().when(configuration.getHttpPublishUri()).thenReturn(URI.create("http://localhost"));

            this.subject = mock(Subject.class);
            lenient().when(subject.isPermitted(anyString())).thenReturn(true);
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return user;
        }
    }

}
