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

import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.BadRequestException;

import java.net.URI;

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
    InputCreateRequest inputCreateRequest;

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
        when(inputCreateRequest.global()).thenReturn(false);

        assertThatThrownBy(() -> inputsResource.create(inputCreateRequest)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only global inputs");
    }

    @Test
    public void testCreateNotCloudCompatibleInputInCloud() throws Exception {
        when(configuration.isCloud()).thenReturn(true);
        when(inputCreateRequest.global()).thenReturn(true);
        when(messageInput.isCloudCompatible()).thenReturn(false);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);

        assertThatThrownBy(() -> inputsResource.create(inputCreateRequest)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed in the cloud environment");
    }

    @Test
    public void testCreateCloudCompatibleInputInCloud() throws Exception {
        when(configuration.isCloud()).thenReturn(true);
        when(inputCreateRequest.global()).thenReturn(true);
        when(messageInput.isCloudCompatible()).thenReturn(true);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);
        when(inputService.save(any())).thenReturn("id");

        assertThat(inputsResource.create(inputCreateRequest).getStatus()).isEqualTo(201);
    }

    @Test
    public void testCreateInput() throws Exception {
        when(configuration.isCloud()).thenReturn(false);
        when(messageInputFactory.create(any(), any(), any())).thenReturn(messageInput);
        when(inputService.save(any())).thenReturn("id");

        assertThat(inputsResource.create(inputCreateRequest).getStatus()).isEqualTo(201);
    }

    static class InputsTestResource extends InputsResource {

        private final User user;

        public InputsTestResource(InputService inputService,
                                  MessageInputFactory messageInputFactory,
                                  Configuration config) {
            super(inputService, messageInputFactory, config);
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
