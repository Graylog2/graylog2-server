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
package org.graylog2.inputs.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.inputs.WithInputConfiguration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueDeserializer;
import org.graylog2.security.encryption.EncryptedValueSerializer;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputConfigurationDeserializerTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        final var messageInputFactory = mock(MessageInputFactory.class);
        final var inputConfig = mock(MessageInput.Config.class);
        final var configRequest = new ConfigurationRequest();
        final var encryptedValueService = new EncryptedValueService("0123456789abcdef");

        configRequest.putAll(Map.of(
                "username", new TextField("username", "", "", "", ConfigurationField.Optional.NOT_OPTIONAL),
                "password", new TextField("password", "", "", "", ConfigurationField.Optional.NOT_OPTIONAL, true)
        ));

        when(messageInputFactory.getConfig(anyString())).thenReturn(Optional.of(inputConfig));

        when(inputConfig.combinedRequestedConfiguration()).thenReturn(configRequest);

        this.objectMapper = new ObjectMapper()
                .registerModule(new GuavaModule()) // for immutable collections
                .registerModule(new SimpleModule("Test")
                        .setDeserializerModifier(new BeanDeserializerModifier() {
                            @Override
                            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                                          BeanDescription beanDesc,
                                                                          JsonDeserializer<?> deserializer) {
                                if (WithInputConfiguration.class.isAssignableFrom(beanDesc.getBeanClass())) {
                                    return new InputConfigurationDeserializer((BeanDeserializer) deserializer, messageInputFactory::getConfig);
                                }

                                return super.modifyDeserializer(config, beanDesc, deserializer);
                            }
                        })
                        .addSerializer(EncryptedValue.class, new EncryptedValueSerializer())
                        .addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer(encryptedValueService)));
    }

    @Test
    void deserializer() throws Exception {
        var value = Value.create("org.graylog2.inputs.FooInput", Map.of("username", "jane", "password", "s3cr3t"));
        var json = f("""
                {
                  "type": "%s",
                  "configuration": {
                    "username": "%s",
                    "password": "%s"
                  }
                }
                """, value.type(), value.configuration().get("username"), value.configuration().get("password"));

        var parsedValue = objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), Value.class);

        assertThat(parsedValue.type()).isEqualTo(value.type());
        assertThat(parsedValue.configuration().get("username")).isEqualTo(value.configuration().get("username"));
        assertThat(parsedValue.configuration().get("password")).isInstanceOf(EncryptedValue.class);
    }

    @Test
    void doesNotAddMissingKeys() throws Exception {
        var value = Value.create("org.graylog2.inputs.FooInput", Map.of("username", "jane", "password", ""));
        var json = f("""
                {
                  "type": "%s",
                  "configuration": {
                    "username": "%s"
                  }
                }
                """, value.type(), value.configuration().get("username"));

        var parsedValue = objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), Value.class);

        assertThat(parsedValue.type()).isEqualTo(value.type());
        assertThat(parsedValue.configuration().get("username")).isEqualTo(value.configuration().get("username"));
        assertThat(parsedValue.configuration()).doesNotContainKey("password");
    }

    @AutoValue
    public static abstract class Value implements WithInputConfiguration<Value> {
        @Override
        @JsonProperty("type")
        public abstract String type();

        @Override
        @JsonProperty("configuration")
        public abstract ImmutableMap<String, Object> configuration();

        @Override
        public Value withConfiguration(Map<String, Object> configuration) {
            return create(type(), configuration);
        }

        @JsonCreator
        public static Value create(@JsonProperty("type") String type,
                                   @JsonProperty("configuration") Map<String, Object> configuration) {
            return new AutoValue_InputConfigurationDeserializerTest_Value(type, ImmutableMap.copyOf(configuration));
        }
    }
}
