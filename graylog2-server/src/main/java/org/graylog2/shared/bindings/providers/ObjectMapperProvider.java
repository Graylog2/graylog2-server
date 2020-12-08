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
package org.graylog2.shared.bindings.providers;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import org.graylog2.jackson.JodaTimePeriodKeyDeserializer;
import org.graylog2.jackson.SemverDeserializer;
import org.graylog2.jackson.SemverRequirementDeserializer;
import org.graylog2.jackson.SemverRequirementSerializer;
import org.graylog2.jackson.SemverSerializer;
import org.graylog2.jackson.VersionDeserializer;
import org.graylog2.jackson.VersionSerializer;
import org.graylog2.plugin.inject.JacksonSubTypes;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueDeserializer;
import org.graylog2.security.encryption.EncryptedValueSerializer;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.jackson.SizeSerializer;
import org.graylog2.shared.plugins.GraylogClassLoader;
import org.graylog2.shared.rest.RangeJsonSerializer;
import org.joda.time.Period;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    protected final ObjectMapper objectMapper;

    // WARNING: This constructor should ONLY be used for tests!
    public ObjectMapperProvider() {
        this(ObjectMapperProvider.class.getClassLoader(), Collections.emptySet(), new EncryptedValueService(UUID.randomUUID().toString()), GRNRegistry.createWithBuiltinTypes());
    }

    // WARNING: This constructor should ONLY be used for tests!
    public ObjectMapperProvider(ClassLoader classLoader, Set<NamedType> subtypes) {
        this(classLoader, subtypes, new EncryptedValueService(UUID.randomUUID().toString()), GRNRegistry.createWithBuiltinTypes());
    }

    @Inject
    public ObjectMapperProvider(@GraylogClassLoader final ClassLoader classLoader,
                                @JacksonSubTypes Set<NamedType> subtypes,
                                EncryptedValueService encryptedValueService,
                                GRNRegistry grnRegistry) {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory typeFactory = mapper.getTypeFactory().withClassLoader(classLoader);
        final AutoValueSubtypeResolver subtypeResolver = new AutoValueSubtypeResolver();

        this.objectMapper = mapper
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)
                .setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy())
                .setSubtypeResolver(subtypeResolver)
                .setTypeFactory(typeFactory)
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false))
                .registerModule(new SimpleModule("Graylog")
                        .addKeyDeserializer(Period.class, new JodaTimePeriodKeyDeserializer())
                        .addKeyDeserializer(GRN.class, new GRNKeyDeserializer(grnRegistry))
                        .addSerializer(new RangeJsonSerializer())
                        .addSerializer(new SizeSerializer())
                        .addSerializer(new ObjectIdSerializer())
                        .addSerializer(new VersionSerializer())
                        .addSerializer(new SemverSerializer())
                        .addSerializer(new SemverRequirementSerializer())
                        .addSerializer(GRN.class, new ToStringSerializer())
                        .addSerializer(EncryptedValue.class, new EncryptedValueSerializer())
                        .addDeserializer(Version.class, new VersionDeserializer())
                        .addDeserializer(Semver.class, new SemverDeserializer())
                        .addDeserializer(Requirement.class, new SemverRequirementDeserializer())
                        .addDeserializer(GRN.class, new GRNDeserializer(grnRegistry))
                        .addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer(encryptedValueService))
                );

        if (subtypes != null) {
            objectMapper.registerSubtypes(subtypes.toArray(new NamedType[]{}));
        }
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }
}
