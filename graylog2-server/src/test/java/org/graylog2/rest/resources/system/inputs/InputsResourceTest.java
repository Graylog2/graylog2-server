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
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.inputs.InputDiagnosticService;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputsResourceTest {

    @Mock
    InputService inputService;

    @Mock
    MessageInputFactory messageInputFactory;

    @Mock
    Configuration configuration;

    @Mock
    MessageInput messageInput;

    InputsResource inputsResource;

    @BeforeEach
    public void setUp() {
        inputsResource = new InputsTestResource(inputService, messageInputFactory, configuration);
    }

    @Test
    public void testCreateNotGlobalInputInCloud() {
        when(configuration.isCloud()).thenReturn(true);

        assertThatThrownBy(() -> inputsResource.create(getCR(false))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only global inputs");
    }

    @Test
    public void testCreateNotCloudCompatibleInputInCloud() throws Exception {
        when(configuration.isCloud()).thenReturn(true);
        when(messageInput.isCloudCompatible()).thenReturn(false);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);

        assertThatThrownBy(() -> inputsResource.create(getCR(true))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed in the cloud environment");
    }

    @Test
    public void testCreateCloudCompatibleInputInCloud() throws Exception {
        when(configuration.isCloud()).thenReturn(true);
        when(messageInput.isCloudCompatible()).thenReturn(true);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);
        when(inputService.save(any())).thenReturn("id");

        assertThat(inputsResource.create(getCR(true)).getStatus()).isEqualTo(201);
    }

    @Test
    public void testCreateInput() throws Exception {
        when(configuration.isCloud()).thenReturn(false);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);
        when(inputService.save(any())).thenReturn("id");

        assertThat(inputsResource.create(getCR(true)).getStatus()).isEqualTo(201);
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

        public InputsTestResource(InputService inputService,
                                  MessageInputFactory messageInputFactory,
                                  Configuration config) {
            super(inputService, mock(InputDiagnosticService.class), messageInputFactory, config);
            configuration = mock(HttpConfiguration.class);
            this.user = mock(User.class);
            lenient().when(user.getName()).thenReturn("foo");
            lenient().when(configuration.getHttpPublishUri()).thenReturn(URI.create("http://localhost"));
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return user;
        }
    }

}
