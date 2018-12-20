package org.graylog2.rest.resources.system.inputs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
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

    private InputsResource inputsResource;

    @Before
    public void setUp() throws Exception {
        this.inputsResource = new InputsResource(inputService, messageInputFactory);
    }

    @Test
    public void testMaskingOfPasswordFields() {
        final ConfigurationField fooInput = mock(ConfigurationField.class);
        final TextField passwordInput = mock(TextField.class);

        when(fooInput.getName()).thenReturn("foo");
        when(passwordInput.getName()).thenReturn("password");
        when(passwordInput.getAttributes()).thenReturn(ImmutableList.of(TextField.Attribute.IS_PASSWORD.toString().toLowerCase()));
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields(fooInput, passwordInput);
        final Map<String, Object> configuration = ImmutableMap.of(
                "foo", 42,
                "password", "verysecret"
        );

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

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

        assertThat(resultingAttributes).containsEntry("nopassword", "lasers in space");
    }

    @Test
    public void testMaskingOfFieldWithoutType() {
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields();
        final Map<String, Object> configuration = ImmutableMap.of(
                "nopassword", "lasers in space"
        );

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).containsEntry("nopassword", "lasers in space");
    }

    @Test
    public void testMaskingOfEmptyMap() {
        final ConfigurationRequest configurationRequest = ConfigurationRequest.createWithFields();
        final Map<String, Object> configuration = Collections.emptyMap();

        final Map<String, Object> resultingAttributes = this.inputsResource.maskPasswordsInConfiguration(configuration, configurationRequest);

        assertThat(resultingAttributes).isEmpty();
    }
}