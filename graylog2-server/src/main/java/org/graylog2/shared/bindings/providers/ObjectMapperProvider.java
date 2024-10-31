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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.grn.GRNRegistry;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.inject.JacksonSubTypes;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;
import org.graylog2.shared.plugins.GraylogClassLoader;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Singleton
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    private static final Logger log = LoggerFactory.getLogger(ObjectMapperProvider.class);

    protected final ObjectMapper objectMapper;

    private final LoadingCache<DateTimeZone, ObjectMapper> mapperByTimeZone = CacheBuilder.newBuilder()
            .maximumSize(DateTimeZone.getAvailableIDs().size())
            .build(
                    new CacheLoader<>() {
                        @Override
                        public ObjectMapper load(@Nonnull DateTimeZone key) {
                            return objectMapper.copy().setTimeZone(key.toTimeZone());
                        }
                    }
            );

    // WARNING: This constructor should ONLY be used for tests!
    public ObjectMapperProvider() {
        this(ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                new EncryptedValueService(UUID.randomUUID().toString()),
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig());
    }

    // WARNING: This constructor should ONLY be used for tests!
    public ObjectMapperProvider(ClassLoader classLoader, Set<NamedType> subtypes) {
        this(classLoader,
                subtypes,
                new EncryptedValueService(UUID.randomUUID().toString()),
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig());
    }

    @Inject
    public ObjectMapperProvider(@GraylogClassLoader final ClassLoader classLoader,
                                @JacksonSubTypes final Set<NamedType> subtypes,
                                final EncryptedValueService encryptedValueService,
                                final GRNRegistry grnRegistry,
                                final InputConfigurationBeanDeserializerModifier inputConfigurationBeanDeserializerModifier) {
        final ObjectMapper mapper = new ObjectMapper();
        this.objectMapper = ObjectMapperConfiguration.configureMapper(mapper,
                classLoader,
                subtypes,
                encryptedValueService,
                grnRegistry,
                inputConfigurationBeanDeserializerModifier);
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }

    /**
     * Returns an ObjectMapper which is configured to use the given time zone.
     * <p>
     * The mapper object is cached, so it must not be modified by the client.
     *
     * @param timeZone The time zone used for dates
     * @return An object mapper with the given time zone configured. If a {@code null} time zone was used, or any
     * exception happend, the default object mapper using the UTC time zone is returned.
     */
    public ObjectMapper getForTimeZone(DateTimeZone timeZone) {
        if (timeZone != null) {
            try {
                return mapperByTimeZone.get(timeZone);
            } catch (Exception e) {
                log.error("Unable to get ObjectMapper for time zone <" + timeZone + ">. Using UTC ObjectMapper instead.", e);
            }
        }
        return objectMapper;
    }
}
