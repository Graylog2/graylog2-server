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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.SimpleType;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.plugins.views.search.views.MongoIgnore;
import org.graylog2.indexer.retention.strategies.UnknownRetentionStrategyConfig;
import org.graylog2.jackson.MongoJodaDateTimeDeserializer;
import org.graylog2.jackson.MongoJodaDateTimeSerializer;
import org.graylog2.jackson.MongoZonedDateTimeDeserializer;
import org.graylog2.jackson.MongoZonedDateTimeSerializer;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.joda.time.DateTime;
import org.mongojack.internal.MongoJackModule;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

@Singleton
public class MongoJackObjectMapperProvider implements Provider<ObjectMapper> {
    private final ObjectMapper objectMapper;

    @Inject
    public MongoJackObjectMapperProvider(ObjectMapper objectMapper) {
        this.objectMapper = configure(objectMapper);
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }

    private ObjectMapper configure(ObjectMapper objectMapper) {
        final var configuredObjectMapper = objectMapper.copy()
                .registerModule(serializationModule())
                .addHandler(new ReplaceUnknownSubtypesWithFallbackHandler())
                .setPropertyNamingStrategy(new PreserveLeadingUnderscoreStrategy());

        EncryptedValueMapperConfig.enableDatabase(configuredObjectMapper);

        MongoJackModule.configure(configuredObjectMapper);

        return configuredObjectMapper;
    }

    private SimpleModule serializationModule() {
        return new SimpleModule("JSR-310-MongoJack")
                .addSerializer(ZonedDateTime.class, new MongoZonedDateTimeSerializer())
                .addDeserializer(ZonedDateTime.class, new MongoZonedDateTimeDeserializer())
                .addSerializer(DateTime.class, new MongoJodaDateTimeSerializer())
                .addDeserializer(DateTime.class, new MongoJodaDateTimeDeserializer())
                .setSerializerModifier(serializerModifier());
    }

    private BeanSerializerModifier serializerModifier() {
        return new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> beanProperties) {
                beanProperties.removeIf(prop -> prop.getAnnotation(MongoIgnore.class) != null);
                return beanProperties;
            }
        };
    }

    /**
     * This abomination is necessary because when using MongoJack to read "_id" object ids back from the database
     * the property name isn't correctly mapped to the POJO using the LowerCaseWithUnderscoresStrategy.
     * <p>
     * Apparently no one in the world tried to use a different naming strategy with MongoJack.
     * (one of my many useless talents is finding corner cases).
     * </p>
     */
    public static class PreserveLeadingUnderscoreStrategy extends PropertyNamingStrategies.SnakeCaseStrategy {
        @Override
        public String translate(String input) {
            String translated = super.translate(input);
            if (input.startsWith("_") && !translated.startsWith("_")) {
                translated = "_" + translated; // lol underscore
            }
            return translated;
        }
    }

    // TODO this should be pluggable to allow subsystems to specify their own fallback types instead of hardcoding it there.
    private static class ReplaceUnknownSubtypesWithFallbackHandler extends DeserializationProblemHandler {
        @Override
        public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) throws IOException {
            if (baseType.getRawClass().equals(RetentionStrategyConfig.class)) {
                return SimpleType.constructUnsafe(UnknownRetentionStrategyConfig.class);
            }
            return super.handleUnknownTypeId(ctxt, baseType, subTypeId, idResolver, failureMsg);
        }
    }
}
