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
package org.graylog.collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog2.Configuration;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorIngestInputServiceTest {

    @Mock private InputService inputService;
    @Mock private MessageInputFactory messageInputFactory;
    @Mock private Configuration configuration;
    @Mock private Subject subject;

    private CollectorIngestInputService service;

    @BeforeEach
    void setUp() {
        service = new CollectorIngestInputService(inputService, messageInputFactory, configuration);
    }

    @Test
    void getInputIdsReturnsIdsForMatchingInputs() {
        final var input1 = mock(org.graylog2.inputs.Input.class);
        final var input2 = mock(org.graylog2.inputs.Input.class);
        when(input1.getId()).thenReturn("id-1");
        when(input2.getId()).thenReturn("id-2");
        when(inputService.allByType(CollectorIngestHttpInput.class.getCanonicalName()))
                .thenReturn(List.of(input1, input2));

        assertThat(service.getInputIds()).containsExactly("id-1", "id-2");
    }

    @Test
    void getInputIdsReturnsEmptyListWhenNoInputs() {
        when(inputService.allByType(CollectorIngestHttpInput.class.getCanonicalName()))
                .thenReturn(List.of());

        assertThat(service.getInputIds()).isEmpty();
    }

    @Test
    void createInputRejectsInCloud() {
        when(configuration.isCloud()).thenReturn(true);

        assertThatThrownBy(() -> service.createInput(subject, "admin", 14401))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not supported in cloud");
    }

    @Test
    void createInputRejectsWithoutInputsCreatePermission() {
        when(subject.isPermitted(RestPermissions.INPUTS_CREATE)).thenReturn(false);

        assertThatThrownBy(() -> service.createInput(subject, "admin", 14401))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createInputRejectsWithoutInputTypePermission() {
        when(subject.isPermitted(RestPermissions.INPUTS_CREATE)).thenReturn(true);
        when(subject.isPermitted(RestPermissions.INPUT_TYPES_CREATE + ":" + CollectorIngestHttpInput.class.getCanonicalName()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.createInput(subject, "admin", 14401))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createInputSucceedsWithPermissions() throws Exception {
        when(subject.isPermitted(RestPermissions.INPUTS_CREATE)).thenReturn(true);
        when(subject.isPermitted(RestPermissions.INPUT_TYPES_CREATE + ":" + CollectorIngestHttpInput.class.getCanonicalName()))
                .thenReturn(true);

        final var messageInput = mock(MessageInput.class);
        when(messageInput.asMap()).thenReturn(Map.of());
        when(messageInputFactory.create(any(InputCreateRequest.class), anyString(), isNull(), anyBoolean()))
                .thenReturn(messageInput);
        final var input = mock(org.graylog2.inputs.Input.class);
        when(inputService.create(any(Map.class))).thenReturn(input);

        service.createInput(subject, "admin", 14401);

        verify(messageInput).checkConfiguration();
        verify(inputService).save(input);
    }

    @Test
    void createInputMapsConfigurationExceptionToBadRequest() throws Exception {
        when(subject.isPermitted(RestPermissions.INPUTS_CREATE)).thenReturn(true);
        when(subject.isPermitted(RestPermissions.INPUT_TYPES_CREATE + ":" + CollectorIngestHttpInput.class.getCanonicalName()))
                .thenReturn(true);

        final var messageInput = mock(MessageInput.class);
        when(messageInputFactory.create(any(InputCreateRequest.class), anyString(), isNull(), anyBoolean()))
                .thenReturn(messageInput);
        org.mockito.Mockito.doThrow(new ConfigurationException("bad config")).when(messageInput).checkConfiguration();

        assertThatThrownBy(() -> service.createInput(subject, "admin", 14401))
                .isInstanceOf(BadRequestException.class);
    }
}
