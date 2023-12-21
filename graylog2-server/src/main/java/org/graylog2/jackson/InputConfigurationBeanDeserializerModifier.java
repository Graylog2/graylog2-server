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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import org.graylog2.inputs.WithInputConfiguration;
import org.graylog2.inputs.jackson.InputConfigurationDeserializer;
import org.graylog2.inputs.jackson.InputConfigurationDeserializer.InputFieldConfigProvider;
import org.graylog2.shared.inputs.MessageInputFactory;

import jakarta.inject.Inject;

import java.util.Optional;

public class InputConfigurationBeanDeserializerModifier extends BeanDeserializerModifier {
    private final InputFieldConfigProvider inputFieldConfigProvider;

    public static InputConfigurationBeanDeserializerModifier withoutConfig() {
        return new InputConfigurationBeanDeserializerModifier(type -> Optional.empty());
    }

    @Inject
    public InputConfigurationBeanDeserializerModifier(MessageInputFactory messageInputFactory) {
        this.inputFieldConfigProvider = messageInputFactory::getConfig;
    }

    public InputConfigurationBeanDeserializerModifier(InputFieldConfigProvider inputFieldConfigProvider) {
        this.inputFieldConfigProvider = inputFieldConfigProvider;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                  BeanDescription beanDesc,
                                                  JsonDeserializer<?> deserializer) {
        if (WithInputConfiguration.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new InputConfigurationDeserializer((BeanDeserializer) deserializer, inputFieldConfigProvider);
        }

        return super.modifyDeserializer(config, beanDesc, deserializer);
    }
}
