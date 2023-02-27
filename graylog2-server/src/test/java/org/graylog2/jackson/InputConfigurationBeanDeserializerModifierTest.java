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
package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.graylog.grn.GRNRegistry;
import org.graylog2.inputs.codecs.JsonPathCodec;
import org.graylog2.inputs.jackson.InputConfigurationDeserializer;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.transports.HttpPollTransport;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;

class InputConfigurationBeanDeserializerModifierTest {

    @Test
    void inputConfigDeserialization() throws JsonProcessingException {
        final String inputConfig = f("""
                {
                    "title": "Test Input",
                    "type": "%s",
                    "configuration": {
                        "headers": "X-Unencrypted: true",
                        "encrypted_headers": "X-Encrypted: true"
                    }
                }
                """, JsonPathInput.class.getCanonicalName());

        InputConfigurationDeserializer.InputFieldConfigProvider configProvider = type -> {
            if (JsonPathInput.class.getCanonicalName().equals(type)) {
                final JsonPathInput.Config config = new JsonPathInput.Config(new HttpPollTransport.Factory() {
                    @Override
                    public HttpPollTransport create(Configuration configuration) {
                        throw new IllegalStateException("Unexpected");
                    }

                    @Override
                    public HttpPollTransport.Config getConfig() {
                        return new HttpPollTransport.Config();
                    }
                }, new JsonPathCodec.Factory() {
                    @Override
                    public JsonPathCodec create(Configuration configuration) {
                        throw new IllegalStateException("Unexpected");
                    }

                    @Override
                    public JsonPathCodec.Config getConfig() {
                        return new JsonPathCodec.Config();
                    }

                    @Override
                    public JsonPathCodec.Descriptor getDescriptor() {
                        throw new IllegalStateException("Unexpected");
                    }
                });
                return Optional.of(config);
            }
            return Optional.empty();
        };

        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider(ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                new EncryptedValueService(UUID.randomUUID().toString()),
                GRNRegistry.createWithBuiltinTypes(),
                new InputConfigurationBeanDeserializerModifier(configProvider));
        final InputCreateRequest inputCreateRequest = objectMapperProvider.get().readValue(inputConfig, InputCreateRequest.class);
        assertThat(inputCreateRequest.configuration()).hasEntrySatisfying("encrypted_headers", e -> {
            assertThat(e).isInstanceOf(EncryptedValue.class);
            EncryptedValue encryptedValue = (EncryptedValue) e;
            assertThat(encryptedValue.isSet()).isTrue();
            assertThat(encryptedValue.value()).isNotBlank();
            assertThat(encryptedValue.salt()).isNotBlank();
        });
    }
}
