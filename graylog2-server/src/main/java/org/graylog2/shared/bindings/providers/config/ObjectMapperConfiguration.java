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
package org.graylog2.shared.bindings.providers.config;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.zafarkhaja.semver.Version;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDeserializer;
import org.graylog.grn.GRNKeyDeserializer;
import org.graylog.grn.GRNRegistry;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.jackson.AutoValueSubtypeResolver;
import org.graylog2.jackson.DeserializationProblemHandlerModule;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.jackson.JacksonModelValidator;
import org.graylog2.jackson.JodaDurationCompatSerializer;
import org.graylog2.jackson.JodaTimePeriodKeyDeserializer;
import org.graylog2.jackson.SemverDeserializer;
import org.graylog2.jackson.SemverRequirementDeserializer;
import org.graylog2.jackson.SemverRequirementSerializer;
import org.graylog2.jackson.SemverSerializer;
import org.graylog2.jackson.VersionDeserializer;
import org.graylog2.jackson.VersionSerializer;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueDeserializer;
import org.graylog2.security.encryption.EncryptedValueSerializer;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.jackson.SizeSerializer;
import org.graylog2.shared.rest.RangeJsonSerializer;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ObjectMapperConfiguration {

    public static <T extends ObjectMapper> T configureMapper(T mapper,
                                                             final ClassLoader classLoader,
                                                             final Set<NamedType> subtypes,
                                                             final EncryptedValueService encryptedValueService,
                                                             final GRNRegistry grnRegistry,
                                                             final InputConfigurationBeanDeserializerModifier inputConfigurationBeanDeserializerModifier) {

        final TypeFactory typeFactory = mapper.getTypeFactory().withClassLoader(classLoader);
        final AutoValueSubtypeResolver subtypeResolver = new AutoValueSubtypeResolver();

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)
                // Starting from Jackson 2.16, the default for INCLUDE_SOURCE_IN_LOCATION was changed to `disabled`.
                // We are explicitly enabling it again to get verbose output that helps with troubleshooting.
                .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy())
                .setSubtypeResolver(subtypeResolver)
                .setTypeFactory(typeFactory)
                .setDateFormat(new StdDateFormat().withColonInTimeZone(false))
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false))
                .registerModule(new DeserializationProblemHandlerModule())
                .registerModule(new SimpleModule("Graylog")
                        .addKeyDeserializer(Period.class, new JodaTimePeriodKeyDeserializer())
                        .addKeyDeserializer(GRN.class, new GRNKeyDeserializer(grnRegistry))
                        .addSerializer(new RangeJsonSerializer())
                        .addSerializer(new SizeSerializer())
                        .addSerializer(new ObjectIdSerializer())
                        .addSerializer(new VersionSerializer())
                        .addSerializer(new SemverSerializer())
                        .addSerializer(new SemverRequirementSerializer())
                        .addSerializer(Duration.class, new JodaDurationCompatSerializer())
                        .addSerializer(GRN.class, new ToStringSerializer())
                        .addSerializer(EncryptedValue.class, new EncryptedValueSerializer())
                        .addDeserializer(Version.class, new VersionDeserializer())
                        .addDeserializer(Semver.class, new SemverDeserializer())
                        .addDeserializer(Requirement.class, new SemverRequirementDeserializer())
                        .addDeserializer(GRN.class, new GRNDeserializer(grnRegistry))
                        .addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer(encryptedValueService))
                        .setDeserializerModifier(inputConfigurationBeanDeserializerModifier)
                        .setSerializerModifier(JacksonModelValidator.getBeanSerializerModifier())
                );

        if (subtypes != null) {
            mapper.registerSubtypes(subtypes.toArray(new NamedType[]{}));
        }

        return mapper;

    }
}
