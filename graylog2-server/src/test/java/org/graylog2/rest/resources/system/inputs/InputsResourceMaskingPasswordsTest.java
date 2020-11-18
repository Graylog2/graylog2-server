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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.subject.Subject;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.rest.models.system.inputs.responses.InputsList;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputsResourceMaskingPasswordsTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private InputService inputService;

    @Mock
    private MessageInputFactory messageInputFactory;

    @Mock
    private Subject currentSubject;

    private Map<String, InputDescription> availableInputs;

    private InputsResource inputsResource;

    class InputsTestResource extends InputsResource {
        public InputsTestResource(InputService inputService, MessageInputFactory messageInputFactory) {
            super(inputService, messageInputFactory);
        }

        @Override
        protected Subject getSubject() {
            return currentSubject;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.availableInputs = new HashMap<>();
        when(messageInputFactory.getAvailableInputs()).thenReturn(this.availableInputs);
        this.inputsResource = new InputsTestResource(inputService, messageInputFactory);
    }

    @Test
    public void testMaskingOfPasswordFields() {
        final ConfigurationField fooInput = mock(ConfigurationField.class);
        final TextField passwordInput = mock(TextField.class);

        when(fooInput.getName()).thenReturn("foo");
        when(passwordInput.getName()).thenReturn("password");
        when(passwordInput.getAttributes()).thenReturn(ImmutableList.of(TextField.Attribute.IS_PASSWORD.toString().toLowerCase(Locale.ENGLISH)));
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(fooInput, passwordInput);
        final Map<String, Object> configuration = ImmutableMap.of(
                "foo", 42,
                "password", "verysecret"
        );

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).hasSize(2);
        assertThat(resultingAttributes).containsEntry("password", "<password set>");
        assertThat(resultingAttributes).containsEntry("foo", 42);
    }

    @Test
    public void testMaskingOfNonPasswordFields() {
        final TextField passwordInput = mock(TextField.class);

        when(passwordInput.getName()).thenReturn("nopassword");
        when(passwordInput.getAttributes()).thenReturn(ImmutableList.of());
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(passwordInput);
        final Map<String, Object> configuration = ImmutableMap.of(
                "nopassword", "lasers in space"
        );

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).hasSize(1);
        assertThat(resultingAttributes).containsEntry("nopassword", "lasers in space");
    }

    @Test
    public void testMaskingOfFieldWithoutType() {
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields();
        final Map<String, Object> configuration = ImmutableMap.of(
                "nopassword", "lasers in space"
        );

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).hasSize(1);
        assertThat(resultingAttributes).containsEntry("nopassword", "lasers in space");
    }

    @Test
    public void testMaskingOfEmptyMap() {
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields();
        final Map<String, Object> configuration = Collections.emptyMap();

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).isEmpty();
    }

    @Test
    public void testMaskingOfNullValueInMap() {
        final TextField passwordInput = mock(TextField.class);

        when(passwordInput.getName()).thenReturn("nopassword");
        when(passwordInput.getAttributes()).thenReturn(ImmutableList.of());
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(passwordInput);
        final Map<String, Object> configuration = Collections.singletonMap("nopassword", null);

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).hasSize(1);
        assertThat(resultingAttributes).containsEntry("nopassword", null);
    }

    @Test
    public void testRetrievalOfInputWithPasswordFieldIfUserIsNotAllowedToEditInput() throws NotFoundException {
        final String inputId = "myinput";
        final String inputType = "dummyinput";

        final Input input = getInput(inputId, inputType);

        when(inputService.find(inputId)).thenReturn(input);

        final ConfigurationField fooInput = mock(ConfigurationField.class);
        when(fooInput.getName()).thenReturn("foo");

        final TextField passwordInput = getPasswordField("password");

        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(fooInput, passwordInput);

        final InputDescription inputDescription = getInputDescription(configurationRequest);
        this.availableInputs.put(inputType, inputDescription);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_READ + ":" + inputId)).thenReturn(true);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_EDIT + ":" + inputId)).thenReturn(false);

        final Map<String, Object> configuration = ImmutableMap.of(
                "foo", 42,
                "password", "verysecret"
        );
        when(input.getConfiguration()).thenReturn(configuration);

        final InputSummary summary = this.inputsResource.get(inputId);

        assertThat(summary.attributes()).hasSize(2);
        assertThat(summary.attributes()).containsEntry("password", "<password set>");
        assertThat(summary.attributes()).containsEntry("foo", 42);
    }

    @Test
    public void testRetrievalOfInputWithPasswordFieldIfUserIsAllowedToEditInput() throws NotFoundException {
        final String inputId = "myinput";
        final String inputType = "dummyinput";

        final Input input = getInput(inputId, inputType);

        when(inputService.find(inputId)).thenReturn(input);

        final ConfigurationField fooInput = mock(ConfigurationField.class);
        when(fooInput.getName()).thenReturn("foo");

        final TextField passwordInput = getPasswordField("password");

        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(fooInput, passwordInput);

        final InputDescription inputDescription = getInputDescription(configurationRequest);
        this.availableInputs.put(inputType, inputDescription);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_READ + ":" + inputId)).thenReturn(true);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_EDIT + ":" + inputId)).thenReturn(true);

        final Map<String, Object> configuration = ImmutableMap.of(
                "foo", 42,
                "password", "verysecret"
        );
        when(input.getConfiguration()).thenReturn(configuration);

        final InputSummary summary = this.inputsResource.get(inputId);

        assertThat(summary.attributes()).hasSize(2);
        assertThat(summary.attributes()).containsEntry("password", "verysecret");
        assertThat(summary.attributes()).containsEntry("foo", 42);
    }

    @Test
    public void testRetrievalOfAllInputsWithPasswordFieldForUserNotAllowedToEditInput() throws NotFoundException {
        final String inputId = "myinput";
        final String inputType = "dummyinput";

        final Input input = getInput(inputId, inputType);

        final ConfigurationField fooInput = mock(ConfigurationField.class);
        when(fooInput.getName()).thenReturn("foo");

        final TextField passwordInput = getPasswordField("password");

        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(fooInput, passwordInput);

        final InputDescription inputDescription = getInputDescription(configurationRequest);
        this.availableInputs.put(inputType, inputDescription);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_READ + ":" + inputId)).thenReturn(true);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_EDIT + ":" + inputId)).thenReturn(false);

        final Map<String, Object> configuration = ImmutableMap.of(
                "foo", 42,
                "password", "verysecret"
        );
        when(input.getConfiguration()).thenReturn(configuration);
        when(inputService.all()).thenReturn(Collections.singletonList(input));

        final InputsList inputsList = this.inputsResource.list();

        assertThat(inputsList.inputs()).isNotEmpty();
        assertThat(inputsList.inputs()).allMatch(summary -> {
            assertThat(summary.attributes()).hasSize(2);
            assertThat(summary.attributes()).containsEntry("password", "<password set>");
            assertThat(summary.attributes()).containsEntry("foo", 42);
            return true;
        });
    }

    @Test
    public void testRetrievalOfAllInputsWithPasswordFieldForUserAllowedToEditInput() throws NotFoundException {
        final String inputId = "myinput";
        final String inputType = "dummyinput";

        final Input input = getInput(inputId, inputType);

        final ConfigurationField fooInput = mock(ConfigurationField.class);
        when(fooInput.getName()).thenReturn("foo");

        final TextField passwordInput = getPasswordField("password");

        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(fooInput, passwordInput);

        final InputDescription inputDescription = getInputDescription(configurationRequest);
        this.availableInputs.put(inputType, inputDescription);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_READ + ":" + inputId)).thenReturn(true);
        when(currentSubject.isPermitted(RestPermissions.INPUTS_EDIT + ":" + inputId)).thenReturn(true);

        final Map<String, Object> configuration = ImmutableMap.of(
                "foo", 42,
                "password", "verysecret"
        );
        when(input.getConfiguration()).thenReturn(configuration);
        when(inputService.all()).thenReturn(Collections.singletonList(input));

        final InputsList inputsList = this.inputsResource.list();

        assertThat(inputsList.inputs()).isNotEmpty();
        assertThat(inputsList.inputs()).allMatch(summary -> {
            assertThat(summary.attributes()).hasSize(2);
            assertThat(summary.attributes()).containsEntry("password", "verysecret");
            assertThat(summary.attributes()).containsEntry("foo", 42);
            return true;
        });
    }

    private TextField getPasswordField(String name) {
        final TextField passwordInput = mock(TextField.class);

        when(passwordInput.getName()).thenReturn(name);
        when(passwordInput.getAttributes()).thenReturn(ImmutableList.of(TextField.Attribute.IS_PASSWORD.toString().toLowerCase(Locale.ENGLISH)));
        return passwordInput;
    }

    private InputDescription getInputDescription(ConfigurationRequest configurationRequest) {
        final InputDescription inputDescription = mock(InputDescription.class);
        when(inputDescription.getConfigurationRequest()).thenReturn(configurationRequest);
        when(inputDescription.getName()).thenReturn("Dummy Input");
        return inputDescription;
    }

    private Input getInput(String inputId, String inputType) {
        final Input input = mock(Input.class);
        when(input.getTitle()).thenReturn("My Input");
        when(input.getType()).thenReturn(inputType);
        when(input.isGlobal()).thenReturn(false);
        when(input.getContentPack()).thenReturn(null);
        when(input.getId()).thenReturn(inputId);
        when(input.getCreatedAt()).thenReturn(DateTime.parse("2018-12-20T15:36:42.234Z"));
        when(input.getType()).thenReturn(inputType);
        when(input.getCreatorUserId()).thenReturn("gary");
        when(input.getStaticFields()).thenReturn(Collections.emptyMap());
        when(input.getNodeId()).thenReturn("deadbeef");
        return input;
    }
}