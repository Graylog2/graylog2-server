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
package org.graylog2.bindings.providers;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.graylog.plugins.views.search.views.MongoIgnore;
import org.graylog2.jackson.MongoJodaDateTimeDeserializer;
import org.graylog2.jackson.MongoJodaDateTimeSerializer;
import org.graylog2.jackson.MongoZonedDateTimeDeserializer;
import org.graylog2.jackson.MongoZonedDateTimeSerializer;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.joda.time.DateTime;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.time.ZonedDateTime;
import java.util.List;

public class CommonMongoJackObjectMapperProvider implements Provider<ObjectMapper> {
    private final Provider<ObjectMapper> objectMapperProvider;

    private static final BeanSerializerModifier customBeanSerializerModifier = new BeanSerializerModifier() {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {
            beanProperties.removeIf(prop -> prop.getAnnotation(MongoIgnore.class) != null);
            return beanProperties;
        }
    };

    private static final com.fasterxml.jackson.databind.Module serializationModule = new SimpleModule("JSR-310-MongoJack")
            .addSerializer(ZonedDateTime.class, new MongoZonedDateTimeSerializer())
            .addDeserializer(ZonedDateTime.class, new MongoZonedDateTimeDeserializer())
            .addSerializer(DateTime.class, new MongoJodaDateTimeSerializer())
            .addDeserializer(DateTime.class, new MongoJodaDateTimeDeserializer())
            .setSerializerModifier(customBeanSerializerModifier);

    @Inject
    public CommonMongoJackObjectMapperProvider(Provider<ObjectMapper> objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }

    @Override
    public ObjectMapper get() {
        return configure(this.objectMapperProvider.get());
    }

    public static ObjectMapper configure(ObjectMapper objectMapper) {
        var configuredObjectMapper = objectMapper.copy()
                .registerModule(serializationModule);

        EncryptedValueMapperConfig.enableDatabase(configuredObjectMapper);

        return configuredObjectMapper;

    }
}
