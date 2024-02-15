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
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog.testing.jackson.JacksonSubtypesAssertions;
import org.graylog2.lookup.caches.CaffeineLookupCache;
import org.graylog2.lookup.caches.NullCache;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

class LookupCacheConfigurationTest {
    @Test
    void subtypes() {

        final var objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(
                new NamedType(CaffeineLookupCache.Config.class, CaffeineLookupCache.NAME),
                new NamedType(NullCache.Config.class, NullCache.NAME)
        );

        final var caffeineCacheConfig = CaffeineLookupCache.Config.builder()
                .type(CaffeineLookupCache.NAME)
                .maxSize(1)
                .expireAfterAccess(1)
                .expireAfterWrite(1)
                .build();

        final var nullCacheConfig = NullCache.Config.builder()
                .type(NullCache.NAME)
                .build();

        JacksonSubtypesAssertions.assertThat(caffeineCacheConfig)
                .withObjectMapper(objectMapper)
                .deserializesWhenGivenSupertype(LookupCacheConfiguration.class)
                .doesNotSerializeWithDuplicateFields();

        JacksonSubtypesAssertions.assertThat(nullCacheConfig)
                .withObjectMapper(objectMapper)
                .deserializesWhenGivenSupertype(LookupCacheConfiguration.class)
                .doesNotSerializeWithDuplicateFields();
    }
}
