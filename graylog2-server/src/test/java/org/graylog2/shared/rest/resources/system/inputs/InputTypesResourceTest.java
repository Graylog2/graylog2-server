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
package org.graylog2.shared.rest.resources.system.inputs;

import org.apache.shiro.subject.Subject;
import org.graylog2.Configuration;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.NotFoundException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputTypesResourceTest {

    private static final String CLOUD_COMPATIBLE = "cloudCompatible";
    private static final String CLOUD_INCOMPATIBLE = "cloudIncompatible";
    private static final String PERMITTED_TYPE = "permittedType";
    private static final String RESTRICTED_TYPE = "restrictedType";
    @Mock
    private MessageInputFactory messageInputFactory;

    @Mock
    private Configuration configuration;

    InputTypesResource inputTypesResource;

    @BeforeEach
    public void setUp() {
        Map<String, InputDescription> inputs = Stream.of(
                mockInput(CLOUD_COMPATIBLE, true),
                mockInput(CLOUD_INCOMPATIBLE, false),
                mockInput(PERMITTED_TYPE, true),
                mockInput(RESTRICTED_TYPE, true)
        ).collect(Collectors.toMap(InputDescription::getName, Function.identity()));
        when(messageInputFactory.getAvailableInputs()).thenReturn(inputs);
        inputTypesResource = new InputTypesTestResource(messageInputFactory, configuration);
    }

    private InputDescription mockInput(String name, boolean isCloudCompatible) {
        InputDescription input = Mockito.mock(InputDescription.class);
        when(input.getName()).thenReturn(name);
        lenient().when(input.isCloudCompatible()).thenReturn(isCloudCompatible);
        lenient().when(input.getLinkToDocs()).thenReturn("link");
        return input;
    }

    @ParameterizedTest
    @CsvSource({"true,2", "false,3"})
    public void testInputTypes(boolean isCloud, int inputCount) {
        when(configuration.isCloud()).thenReturn(isCloud);

        assertThat(inputTypesResource.types().types()).hasSize(inputCount);
    }

    @ParameterizedTest
    @CsvSource({"true,2", "false,3"})
    public void testInputTypesAll(boolean isCloud, int inputCount) {
        when(configuration.isCloud()).thenReturn(isCloud);

        assertThat(inputTypesResource.all()).hasSize(inputCount);
    }

    @Test
    public void testInputInfoCloud() {
        when(configuration.isCloud()).thenReturn(true);

        assertThat(inputTypesResource.info(CLOUD_COMPATIBLE).name()).isEqualTo(CLOUD_COMPATIBLE);
        assertThatThrownBy(() -> inputTypesResource.info(CLOUD_INCOMPATIBLE)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining(CLOUD_INCOMPATIBLE);
    }

    @Test
    public void testInputInfoNotCloud() {
        when(configuration.isCloud()).thenReturn(false);

        assertThat(inputTypesResource.info(CLOUD_INCOMPATIBLE).name()).isEqualTo(CLOUD_INCOMPATIBLE);
        assertThat(inputTypesResource.info(CLOUD_COMPATIBLE).name()).isEqualTo(CLOUD_COMPATIBLE);
    }

    @Test
    public void testInputInfoPermitted() {
        assertThat(inputTypesResource.info(PERMITTED_TYPE).name()).isEqualTo(PERMITTED_TYPE);
    }

    @Test
    public void testInputInfoRestricted() {
        assertThatThrownBy(() -> inputTypesResource.info(RESTRICTED_TYPE)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining(RESTRICTED_TYPE);
    }

    static class InputTypesTestResource extends InputTypesResource {

        private final Subject subject;

        public InputTypesTestResource(MessageInputFactory messageInputFactory,
                                      Configuration configuration) {
            super(messageInputFactory, configuration);
            this.subject = mock(Subject.class);
            lenient().doReturn(true).when(subject).isPermitted(anyString());
            lenient().doReturn(false).when(subject).isPermitted(RestPermissions.INPUT_TYPES_READ + ":" + RESTRICTED_TYPE);
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }

    }

}
