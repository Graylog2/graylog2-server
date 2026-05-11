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
package org.graylog2.shared.inputs;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class InputFieldEncryptionValidatorTest {

    @Test
    void noOffendersWhenAllPasswordFieldsAreEncrypted() {
        final MessageInputFactory factory = factoryWithType("ok-type", ConfigurationRequest.createWithFields(
                new TextField("good_password", "Good", "", "",
                        ConfigurationField.Optional.NOT_OPTIONAL, true, TextField.Attribute.IS_PASSWORD),
                new TextField("plain_text", "Plain", "", "",
                        ConfigurationField.Optional.OPTIONAL),
                new NumberField("a_number", "Number", 0, "")
        ));

        assertThat(InputFieldEncryptionValidator.findOffenders(factory)).isEmpty();
    }

    @Test
    void reportsPasswordFieldThatIsNotEncrypted() {
        final MessageInputFactory factory = factoryWithType("bad-type", ConfigurationRequest.createWithFields(
                new TextField("bad_password", "Bad", "", "",
                        ConfigurationField.Optional.NOT_OPTIONAL, false, TextField.Attribute.IS_PASSWORD)
        ));

        assertThat(InputFieldEncryptionValidator.findOffenders(factory))
                .containsExactly("bad-type field \"bad_password\"");
    }

    @Test
    void encryptedFieldWithoutPasswordAttributeIsNotReported() {
        final MessageInputFactory factory = factoryWithType("encrypted-only", ConfigurationRequest.createWithFields(
                new TextField("encrypted_plain_input", "Secret", "", "",
                        ConfigurationField.Optional.NOT_OPTIONAL, true)
        ));

        assertThat(InputFieldEncryptionValidator.findOffenders(factory)).isEmpty();
    }

    @Test
    void plainPasswordFieldsAcrossMultipleTypesAreAllReported() {
        final MessageInputFactory factory = Mockito.mock(MessageInputFactory.class);
        when(factory.getAvailableInputs()).thenReturn(Map.of(
                "type-a", Mockito.mock(InputDescription.class),
                "type-b", Mockito.mock(InputDescription.class)));
        when(factory.getConfig("type-a")).thenReturn(Optional.of(configWith(ConfigurationRequest.createWithFields(
                new TextField("pw1", "A", "", "",
                        ConfigurationField.Optional.NOT_OPTIONAL, false, TextField.Attribute.IS_PASSWORD)))));
        when(factory.getConfig("type-b")).thenReturn(Optional.of(configWith(ConfigurationRequest.createWithFields(
                new TextField("pw2", "B", "", "",
                        ConfigurationField.Optional.NOT_OPTIONAL, false, TextField.Attribute.IS_PASSWORD)))));

        assertThat(InputFieldEncryptionValidator.findOffenders(factory))
                .containsExactlyInAnyOrder(
                        "type-a field \"pw1\"",
                        "type-b field \"pw2\""
                );
    }

    @Test
    void unrelatedExceptionsFromConfigDoNotAbortTheScan() {
        final MessageInputFactory factory = Mockito.mock(MessageInputFactory.class);
        when(factory.getAvailableInputs()).thenReturn(Map.of(
                "blows-up", Mockito.mock(InputDescription.class),
                "fine-type", Mockito.mock(InputDescription.class)));

        final MessageInput.Config blowingUp = new MessageInput.Config(null, null) {
            @Override
            public ConfigurationRequest combinedRequestedConfiguration() {
                throw new RuntimeException("boom");
            }
        };
        when(factory.getConfig("blows-up")).thenReturn(Optional.of(blowingUp));
        when(factory.getConfig("fine-type")).thenReturn(Optional.of(configWith(ConfigurationRequest.createWithFields(
                new TextField("pw", "F", "", "",
                        ConfigurationField.Optional.NOT_OPTIONAL, false, TextField.Attribute.IS_PASSWORD)))));

        assertThat(InputFieldEncryptionValidator.findOffenders(factory))
                .containsExactly("fine-type field \"pw\"");
    }

    private static MessageInputFactory factoryWithType(String type, ConfigurationRequest request) {
        final MessageInputFactory factory = Mockito.mock(MessageInputFactory.class);
        when(factory.getAvailableInputs()).thenReturn(Map.of(type, Mockito.mock(InputDescription.class)));
        when(factory.getConfig(type)).thenReturn(Optional.of(configWith(request)));
        return factory;
    }

    private static MessageInput.Config configWith(ConfigurationRequest request) {
        return new MessageInput.Config(null, null) {
            @Override
            public ConfigurationRequest combinedRequestedConfiguration() {
                return request;
            }
        };
    }
}
